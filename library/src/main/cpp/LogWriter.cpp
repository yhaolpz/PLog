//
// Created by Allen on 2017/11/7.
//
#include "LogWriter.h"
#include "ErrInfo.h"
#include <iostream>
#include <sys/file.h>

LogWriter::LogWriter() {

}

ErrInfo *LogWriter::initMmap(JNIEnv *env, std::string basicInfo, std::string logDir) {
    this->basicInfo = basicInfo;
    this->logDir = logDir;
    this->buildDate = getDate();
    // add the suffix '-mmap', to make a distinction from common IO
    this->filePath = logDir + "/" + buildDate + "-mmap";

    this->fd = open(filePath.c_str(), O_RDWR | O_CREAT, (mode_t) 0600);

    if (fd == -1) {
        return new ErrInfo(OPEN_EXIT, "Error opening file");
    }

    this->fileStat.st_size = 0;
    if (fstat(fd, &fileStat) == -1) {
        close(fd);
        return new ErrInfo(FSTAT_EXIT, "Error fstat file");
    }

    this->fileSize = fileStat.st_size;
    this->logPageSize = static_cast<off_t >(ALLOC_PAGE_NUM * sysconf(_SC_PAGE_SIZE));

    bool fileExists = true;

    // If fileSize is not an integer multiple of logPageSize, let it be complemented to an integer multiple of logPageSize
    if (fileSize < logPageSize || fileSize % logPageSize != 0) {

        fileExists = fileSize > 0;

        off_t increaseSize = logPageSize - (fileSize % logPageSize);

        if (ftruncate(fd, fileSize + increaseSize) == -1) {
            close(fd);
            return new ErrInfo(LSEEK_EXIT, "Error when calling ftruncate() to stretch the file");
        }

        fileSize += increaseSize;

        if (lseek(fd, fileSize - 1, SEEK_SET) == -1) {
            close(fd);
            return new ErrInfo(LSEEK_EXIT, "Error calling lseek() to stretch the file");
        }

        if (write(fd, "", sizeof(char)) == -1) {
            close(fd);
            return new ErrInfo(WRITE_EXIT, "Error writing last byte of the file");
        }

    }

    void *map = mmap(NULL, static_cast<size_t>(logPageSize),
                     PROT_READ | PROT_WRITE,
                     MAP_SHARED, fd,
                     fileSize - logPageSize);
    ////////////////////////////////////////////////////////////////////////////////


    if (map == MAP_FAILED || map == NULL) {
        close(fd);
        return new ErrInfo(MMAP_EXIT, "Error mmaping the file");
    }

    recordPtr = static_cast<char *> (map);

    if (recordPtr == NULL) {
        close(fd);
        return new ErrInfo(MMAP_EXIT, "Error cast char*");
    }

    ErrInfo *errInfo = checkMmapFile();
    if (errInfo != NULL) {
        unixMunmap(fd, static_cast<void *>(recordPtr), logPageSize);
        close(fd);
        return errInfo;
    }

    bool findFlag = false;

    for (off_t i = logPageSize - 1; i >= 0; i--) {
        // Find the first '\n' and stop the search, if not found, then the page is still blank, just back to the beginning of the page
        if (recordPtr[i] == '\n') {
            findFlag = true;
            if (i != logPageSize - 1) {
                recordIndex = i + 1;
            } else {
                recordIndex = logPageSize;
            }
            break;
        }
    }
    if (!findFlag) {
        recordIndex = 0;
    }

    memset(recordPtr + recordIndex, 0, static_cast<size_t>(logPageSize - recordIndex));

    // must write basic info to log file if first create
    if (!fileExists) {
        return writeLog(env, basicInfo.c_str(), false);
    }

    return nullptr;
}

/**
 * @param basicInfo
 * @param logDir
 */
ErrInfo *LogWriter::init(JNIEnv *env, std::string basicInfo, std::string logDir) {
    return initMmap(env, basicInfo, logDir);
}

LogWriter::~LogWriter() {

    //now write it to disk
    if (msync(recordPtr, static_cast<size_t>(logPageSize), MS_SYNC) == -1) {
        perror("Could not sync the file to disk");
    }

    //Don't forget to free mmapped memory.
    if (munmap(recordPtr, static_cast<size_t>(logPageSize)) == -1) {
        close(fd);
        perror("Error un-mmaping the file");
        exit(EXIT_FAILURE);
    }
    //Un-mapping doesn't close the file, so we still need to do that.
    close(fd);

    buildDate.shrink_to_fit();
    basicInfo.shrink_to_fit();
    logDir.shrink_to_fit();
    filePath.shrink_to_fit();
}

ErrInfo *LogWriter::writeLog(JNIEnv *env, const char *logMsg) {

    const size_t textSize = strlen(logMsg);
    return writeLog(env, logMsg, textSize);
}

ErrInfo *LogWriter::writeLog(JNIEnv *env, const char *logMsg, size_t textSize) {
    if (logMsg == NULL || textSize <= 0) {
        return nullptr;
    }

    if (recordPtr == NULL) {
        close(fd);
        return new ErrInfo(WRITE_EXIT, "Error writing log");
    }

    ErrInfo *errInfo = checkMmapFile();
    if (errInfo != NULL) {
        unixMunmap(fd, static_cast<void *>(recordPtr), logPageSize);
        close(fd);
        return errInfo;
    }

    size_t msgIndex = 0;

    while (1) {

        for (; msgIndex < textSize && recordIndex < logPageSize; msgIndex++) {
            recordPtr[recordIndex] = logMsg[msgIndex];
            recordIndex++;
        }

        //当开辟的mmap内存被写满时,需要再开辟一页mmap内存
        if (recordIndex >= logPageSize) {

            ErrInfo *errInfo = unixMunmap(fd, recordPtr, (size_t) logPageSize);
            if (errInfo != NULL) {
                close(fd);
                return errInfo;
            }

            recordPtr = NULL;

            if (access(filePath.c_str(), 0) != 0) {
                close(fd);
                return new ErrInfo(ACCESS_EXIT, "Error calling access file");
            }

            //扩展文件大小
            if (ftruncate(fd, fileSize + logPageSize) == -1) {
                close(fd);
                return new ErrInfo(LSEEK_EXIT, "Error calling ftruncate() to stretch file");
            }

            //移动到文件末尾
            if (lseek(fd, fileSize + logPageSize - 1, SEEK_SET) == -1) {
                close(fd);
                return new ErrInfo(LSEEK_EXIT, "Error calling lseek() to stretch the file");
            }

            //在文件末尾写入一个字符，达到扩展文件大小的目的
            if (write(fd, "", 1) == -1) {
                close(fd);
                return new ErrInfo(WRITE_EXIT, "Error writing last byte of the file");
            }

            this->fileStat.st_size = 0;

            if (fstat(fd, &fileStat) == -1) {
                close(fd);
                return new ErrInfo(FSTAT_EXIT, "Error fstat file");
            }

            if (fileStat.st_size - logPageSize != this->fileSize &&
                fileStat.st_size % logPageSize != 0) {
                close(fd);
                return new ErrInfo(WRITE_EXIT, "Error stretch file when writing");
            }

            this->fileSize = fileStat.st_size;

            void *map = mmap(NULL, static_cast<size_t>(logPageSize), PROT_READ | PROT_WRITE,
                             MAP_SHARED, fd,
                             fileSize - logPageSize);

            if (map == MAP_FAILED || map == NULL) {
                close(fd);
                return new ErrInfo(MMAP_EXIT, "Error mmaping the file");
            }

            recordPtr = static_cast<char *> (map);

            if (recordPtr == NULL) {
                close(fd);
                return new ErrInfo(MMAP_EXIT, "Error cast char*");
            }

            memset(recordPtr, 0, static_cast<size_t >(logPageSize));

            recordIndex = 0;
        } else {
            break;
        }
    }

    return nullptr;
}


void LogWriter::refreshBasicInfo(JNIEnv *env, std::string basicInfo) {
    this->basicInfo.shrink_to_fit();
    this->basicInfo = basicInfo;
}

ErrInfo *LogWriter::closeAndRenew(JNIEnv *env, jboolean uploadAction) {

    //还是改成复制一个文件出来更好,比如将2017-11-05复制出一个2017-11-05-up的文件出来
    //首先取消映射
    ErrInfo *errInfo = unixMunmap(fd, recordPtr, static_cast<size_t >(logPageSize));
    if (errInfo != NULL) {
        close(fd);
        return errInfo;
    }
    recordPtr = NULL;
    //然后关闭文件
    close(fd);
    //然后重命名文件
    std::string upFilePath = logDir + "/" + buildDate + "-mmap-up";
    std::string oldUpFilePath = logDir + "/" + buildDate + "-mmap-up-old";
    std::string lastUpFilePath = logDir + "/" + buildDate + "-mmap-up-last";
    if (access(upFilePath.c_str(), 0) == 0) { //如果已经存在mmap-up日志
        if (uploadAction) { //如果是上传行为
            if (access(oldUpFilePath.c_str(), 0) == 0) {
                // MAX < SIZE < MAX*3/2
                if (access(filePath.c_str(), 0) == 0) {
                    rename(filePath.c_str(), lastUpFilePath.c_str());
                }
            } else {
                rename(upFilePath.c_str(), oldUpFilePath.c_str());
                if (access(filePath.c_str(), 0) == 0) {
                    rename(filePath.c_str(), upFilePath.c_str());
                }
            }
        } else { //把mmap-up命名为mmap-up-old
            if (access(oldUpFilePath.c_str(), 0) == 0) {
                remove(oldUpFilePath.c_str());
            }
            rename(upFilePath.c_str(), oldUpFilePath.c_str());
            if (access(filePath.c_str(), 0) == 0) {
                rename(filePath.c_str(), upFilePath.c_str());
            }
        }
    } else {
        if (access(filePath.c_str(), 0) == 0) {
            rename(filePath.c_str(), upFilePath.c_str());
        }
    }
    upFilePath.shrink_to_fit();
    oldUpFilePath.shrink_to_fit();
    lastUpFilePath.shrink_to_fit();
    buildDate.shrink_to_fit();
    filePath.shrink_to_fit();
    //最后重新初始化，即新建文件并映射
    return initMmap(env, basicInfo, logDir);
}

std::string LogWriter::getDate() {
    time_t now = time(0);
    tm localTime = *localtime(&now);
    std::string *date;
    size_t bufSize = sizeof(char) * 20;
    char *buf = (char *) malloc(bufSize);
    strftime(buf, bufSize, "%Y-%m-%d", &localTime);
    date = new std::string(buf);
    free(buf);
    return *date;
}

ErrInfo *LogWriter::unixMunmap(int fd, void *map, size_t map_size) {
    if (msync(map, map_size, MS_SYNC) == -1) {
        return new ErrInfo(UNMMAP_EXIT, "Error sync the file to disk");
    }
    if (munmap(map, map_size) == -1) {
        return new ErrInfo(UNMMAP_EXIT, "Error un-mmapping the file");
    }
    return NULL;
}

ErrInfo *LogWriter::checkMmapFile() {
    if (access(filePath.c_str(), 0) != 0) {
        return new ErrInfo(WRITE_EXIT, "Error access log file");
    }
    this->fileStat.st_size = 0;
    if (fstat(fd, &fileStat) == -1 || this->fileStat.st_size != this->fileSize) {
        return new ErrInfo(FSTAT_EXIT, "Error read file size");
    }
    return NULL;
}

jlong *LogWriter::getFileSize() {
    return reinterpret_cast<jlong *>(this->fileSize);
}

