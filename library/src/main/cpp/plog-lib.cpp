#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include "LogWriter.h"

#ifndef TROJAN_LOG_WRITER_H
#define TROJAN_LOG_WRITER_H

#ifdef __cplusplus
extern "C" {
#endif

std::string jstring2string(JNIEnv *env, jstring jStr) {
    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jstring utf = env->NewStringUTF("UTF-8");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, utf);

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte *pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *) pBytes, length);

    env->DeleteLocalRef(stringClass);
    env->DeleteLocalRef(utf);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_COMMIT);

    return ret;
}

jlong Java_com_wyh_plog_record_impl_MmapLogWriter_nativeInit(JNIEnv *env,
                                                              jobject object,
                                                              jstring basic_info,
                                                              jstring dir) {

    //////////////////////////////////////
    std::string methodName = "nativeInit";
    print(env, methodName.c_str());
    ////////////////////////////////////

    LogWriter *logWriter = new LogWriter();
    std::string basicInfo;
    if (basic_info != NULL) {
        basicInfo = jstring2string(env, basic_info);
        env->DeleteLocalRef(basic_info);
    }
    std::string logDir;
    if (dir != NULL) {
        logDir = jstring2string(env, dir);
        env->DeleteLocalRef(dir);
    }

    ErrInfo *errInfo = logWriter->init(env, basicInfo, logDir);
    if (errInfo != NULL) {
        throwExceptionIfNeed(env, errInfo);
        delete errInfo;
        errInfo = NULL;
    }

    return reinterpret_cast<jlong>(logWriter);
}

jlong Java_com_wyh_plog_record_impl_MmapLogWriter_nativeWrite(JNIEnv *env,
                                                               jobject object,
                                                               jlong log_writer_object,
                                                               jstring msg_content) {

    LogWriter *logWriter = reinterpret_cast<LogWriter *>(log_writer_object);
    if (msg_content != NULL) {
        const char *msg = env->GetStringUTFChars(msg_content, JNI_FALSE);
        ErrInfo *errInfo = logWriter->writeLog(env, msg);
        env->ReleaseStringUTFChars(msg_content, msg);
        if (errInfo != NULL) {
            throwExceptionIfNeed(env, errInfo);
            delete errInfo;
            errInfo = NULL;
        }
    }
    return reinterpret_cast<jlong>(logWriter);
}

void Java_com_wyh_plog_record_impl_MmapLogWriter_nativeRefreshBasicInfo(JNIEnv *env,
                                                                         jobject object,
                                                                         jlong log_writer_object,
                                                                         jstring basic_info) {
    //////////////////////////////////////
    std::string methodName = "nativeRefreshBasicInfo";
    print(env, methodName.c_str());
    ////////////////////////////////////

    std::string basicInfo = jstring2string(env, basic_info);
    LogWriter *logWriter = reinterpret_cast<LogWriter *>(log_writer_object);
    logWriter->refreshBasicInfo(env, basicInfo);
}

/**
 *
 * @param keep  如果已经有重名的 mmap-up 日志，是否保留之
 * 上传日志时需要保留，单天日志文件超过上限时不要保留
 */
void Java_com_wyh_plog_record_impl_MmapLogWriter_nativeCloseAndRenew(JNIEnv *env, jobject object,
                                                                      jlong logWriterObject,
                                                                      jboolean uploadAction) {

    //////////////////////////////////////
    std::string methodName = "nativeCloseAndRenew";
    print(env, methodName.c_str());
    ////////////////////////////////////

    LogWriter *logWriter = reinterpret_cast<LogWriter *>(logWriterObject);
    ErrInfo *errInfo = logWriter->closeAndRenew(env, uploadAction);
    if (errInfo != NULL) {
        throwExceptionIfNeed(env, errInfo);
        delete errInfo;
        errInfo = NULL;
    }
}

#ifdef __cplusplus
};
#endif


#endif

extern "C"
JNIEXPORT jlong JNICALL
Java_com_wyh_plog_record_impl_MmapLogWriter_nativeGetFileSize(JNIEnv *env, jobject instance,
                                                               jlong log_writer_object) {

    LogWriter *logWriter = reinterpret_cast<LogWriter *>(log_writer_object);
    return reinterpret_cast<jlong>(logWriter->getFileSize());
}