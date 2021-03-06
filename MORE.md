# PLog


## 日志写入


##### 日志文件类型


为更好的区分并管理日志，将从写入到上传经历不同状态的日志文件划分为以下类型：


- “-mmap” 正在写入的日志文件，不可上传，文件命名为: yyyy-MM-dd-mmap
- "-mmap-up" 写入完成的日志文件，可压缩上传，文件命名为: yyyy-MM-dd-mmap-up
- "-mmap-up-old" 单天较早写入完成的日志大小，可压缩上传，文件命名为: yyyy-MM-dd-mmap-up-old
- "-mmap-up-last" 触发上传时超出存储上限部分的日志文件，上传操作的中间态，不会长久存在，可压缩上传，文件命名为: yyyy-MM-dd-mmap-up-last
- ".zip" 已压缩加密的日志文件，可直接上传，文件命名为: yyyy-MM-dd HH:mm:ss.zip


##### 控制日志时效


暂定保留最近三天的日志，更早的日志将会在特定时机自动清除当前进程目录所有类型过期的日志文件，这些时机包括：


- 初始化时
- 触发上传重命名日志文件添加"-up"时
- 触发上传压缩所有标记为"-up"的可上传的日志时
- 触发上传扫描所有".zip"可上传的日志时


##### 控制存储上限

理论上我们应该避免无意义的日志记录滥用，控制日志体积，但为避免极端情况下大量日志写入导致存储爆炸，通过将日志文件分为两片控制存储上限。


譬如规定单天日志存储体积上限为 MAX，逻辑大致为下：


- 当天日志文件 -mmap 写入体积达 MAX/3 时就会停止写入，并命名为 -mmap-up，然后新建文件 -mmap 继续写入
- 当写入达 MAX/3 时，先将已存在的 -mmap-up 命名为 -mmap-up-old，再将刚写满的 -mmap 命名为 -mmap-up，然后新建文件 -mmap 继续写入
- 当写入达 MAX/3 时，先删除 -mmap-up-old 文件，将已存在的 -mmap-up 命名为 -mmap-up-old，再将刚写满的 -mmap 命名为 -mmap-up，然后新建文件 -mmap 继续写入


按照此逻辑，假定当天写入日志体积为 SIZE，可以将单天日志存储情况划分为以下几种状态：


- a. SIZE < MAX/3
- b. MAX/3 < SIZE < MAX*2/3
- c. MAX*2/3 < SIZE < MAX


当处于 a 状态时触发上传： -mmap --> -mmap-up --> zip
当处于 b 状态时触发上传： -mmap-up --> -mmap-up-old --> zip ;  -mmap --> -mmap-up --> zip
当处于 c 状态时触发上传： -mmap-up-old --> zip ;  -mmap-up --> zip ;  -mmap --> -mmap-up-last --> zip


为了防止频繁的判断日志体积，可设置每隔一定写入条数后判断一次进行优化，目前设置间隔为 1000 条。








































