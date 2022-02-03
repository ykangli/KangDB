# KangDB
---

# 准备工作

## 背景

​		作为非科班转码小菜鸡，一直感觉计算机的专业知识是自己的短板，而且只看视频学习很难真正理解和掌握相关指点，始终有一种好像懂了，又好像没懂的感觉，况且自己是那种不实践就不能深刻理解知识点的人，那么【造轮子】对自己来说好像是一个不错的方法了吧......著名带逛大师 **轮子哥（vczh）**将绝大部分时间都花在了「轮子」上，为了学习轮子哥的~~带逛本领~~编程能力，利用寒假时间从0 开始手写一个简单的数据库。恰好最近某天无聊逛牛客时，看到有大佬 [[何人听我楚狂声 MYDB——一个简易的数据库实现完整教程](https://www.nowcoder.com/discuss/825665?source_id=profile_create_nctrack&channel=-1)使用Java手写了数据库，有源码+文档，那还说啥，赶紧利用寒假在家的时间撸起来！！！

​		就叫它 **KangDB** 吧~

## 整体架构

​		KangDB分为后端和前端，前后端通过 socket 进行交互。

### 前端

​		前端（客户端）就是读取用户输入，并发送到后端执行，输出返回结果，并等待下一次输入。

### 后端

​		KangDB后端则需要解析 SQL，如果是合法的 SQL，就尝试执行并返回结果。不包括解析器，KangDB的后端划分为**五个模块**，每个模块都又一定的职责，通过接口向其依赖的模块提供方法。五个模块如下所示：

![image-20220127112245276](https://ykangliblog.oss-cn-beijing.aliyuncs.com/article/image-20220127112245276.png)

每个模块的职责如下：

- TM 通过维护 XID 文件来维护事务的状态，并提供接口供其他模块来查询某个事务的状态。 

- DM 直接管理数据库 DB 文件和日志文件。

  DM 的主要职责有：

  1. 分页管理 DB 文件，并进行缓存；

  2) 管理日志文件，保证在发生错误时可以根据日志进行恢复；
  3) 抽象 DB 文件为 DataItem 供上层模块使用，并提供缓存。 

- VM 基于**两段锁协议**实现了调度序列的可串行化，并实现了 **MVCC** 以消除读写阻塞，同时实现了两种隔离级别（**READ COMMITTED** 和 **REPEATABLE READ** ）。

- IM 实现了基于 B+ 树的索引，目前 where 只支持已索引字段。 

- TBM 实现了对字段和表的管理。同时，解析 SQL 语句，并根据语句操作表。

## 开发环境和运行环境

​		项目开发时使用的 **WSL2** 和 **JDK17**，如果要在 Windows 上执行，请替换启动参数中的路径为 Windows，**JDK 版本要保证在 11 或以上，不兼容 JDK 8**。

### JDK

​		一般电脑都安装有JDK 8，关于如何在电脑中已有 JDK 8 的基础上再安装其他版本的 JDK ，并且能随意切换，详细教程见 [电脑已有jdk 8，再配置一个jdk 17](https://ykangli.top/2022/01/26/JDK-Configuration/)

### WSL

​		**WSL** 全称 **Windows Subsystem for Linux**，官方翻译“**适用于Linux的Windows子系统**”。以下是微软官方对WSL的描述:

您可以：

- [在 Microsoft Store](https://aka.ms/wslstore) 中选择你偏好的 GNU/Linux 分发版。
- 运行常用的命令行软件工具（例如 `grep`、`sed`、`awk`）或其他 ELF-64 二进制文件。
- 运行 Bash shell 脚本和 GNU/Linux 命令行应用程序，包括：
  - 工具：vim、emacs、tmux
  - 语言：[NodeJS](https://docs.microsoft.com/zh-cn/windows/nodejs/setup-on-wsl2)、Javascript、[Python](https://docs.microsoft.com/zh-cn/windows/python/web-frameworks)、Ruby、C/C++、C# 与 F#、Rust、Go 等
  - 服务：SSHD、[MySQL](https://docs.microsoft.com/zh-cn/windows/wsl/tutorials/wsl-database)、Apache、lighttpd、[MongoDB](https://docs.microsoft.com/zh-cn/windows/wsl/tutorials/wsl-database)、[PostgreSQL](https://docs.microsoft.com/zh-cn/windows/wsl/tutorials/wsl-database)。
- 使用自己的 GNU/Linux 分发包管理器安装其他软件。
- 使用类似于 Unix 的命令行 shell 调用 Windows 应用程序。
- 在 Windows 上调用 GNU/Linux 应用程序。

#### 什么是 WSL 2？

​		WSL 2 是适用于 Linux 的 Windows 子系统体系结构的一个新版本，它支持适用于 Linux 的 Windows 子系统在 Windows 上运行 ELF64 Linux 二进制文件。 它的主要目标是**提高文件系统性能**，以及添加**完全的系统调用兼容性**。

​		这一新的体系结构改变了这些 Linux 二进制文件与Windows 和计算机硬件进行交互的方式，但仍然提供与 WSL 1（当前广泛可用的版本）中相同的用户体验。

​		单个 Linux 分发版可以在 WSL 1 或 WSL 2 体系结构中运行。 每个分发版可随时升级或降级，并且你可以并行运行 WSL 1 和 WSL 2 分发版。 WSL 2 使用全新的体系结构，该体系结构受益于运行真正的 Linux 内核。

#### 为什么要使用WSL&WSL 2 ？

​		我们日常工作学习中有很多涉及与Linux系统的交互，课程中也有大量的以Linux为基础的知识和命令，如果我们日常工作与使用环境就是Linux那还好，无需切换就可无缝对接。但Windows毕竟是市场占有率最高的操作系统，有大量的人群办公\日常环境需Windows，而开发环境需要Linux。这就产生了一些**在Windows环境下使用Linux的需求**。面对这个需求目前有如下集中常用解决方案：

| 方案             | 优点                         | 缺点                                    |
| ---------------- | ---------------------------- | --------------------------------------- |
| 单主机安装双系统 | 真正的操作系统               | 切换麻烦需要重启                        |
| 双主机安装双系统 | 物理隔离                     | 成本高需要两台PC                        |
| 远程服务器       | 真实的操作系统               | 性能、带宽等局限                        |
| 虚拟机安装Linux  | 完整的使用体验               | 资源消耗大、启动慢、运行效率低          |
| WSL              | 资源消耗小、启动快、无缝衔接 | 使用体验可能不完整，某些Linux软件不支持 |

WSL方案是微软近两年拥抱开源后推出的一个非常棒的解决方案；使用WSL，Windows与Linux子系统将共用同一文件系统，Windows创建的文件Linux子系统也可以访问并修改，反之亦然。我们可以在WSL中使用三剑客命令查询分析windows文档、日志、使用shell命令或者bash脚本运行存储在windows中的linux程序、甚至在WSL中创建docker容器，在windows下使用docker desktop进行可视化管理。特别方便有双系统环境来回切换需求的人。总之**使用WSL既可以使用windows的图形化操作界面又可以使用Linux便捷的命令行工具**，很好的满足了我在windows下使用linux的需求。接下来我将介绍如何打造及使用Winux系统。

#### 安装WSL

参照官方文档[安装 WSL](https://docs.microsoft.com/zh-cn/windows/wsl/install)
