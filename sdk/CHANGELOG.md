## Version 2.0.7 (2020-1-25)
### Changed:
- Ping FE间隔调大至3分钟，部分WIFI情况下，4分钟可能出现无数据交互，长连接断开等情况


## Version 2.0.6 (2021-1-7)
### Added:
- 新增onPullNotification回调接口


## Version 2.0.5 (2020-3-3)
### Added:
- 支持日志上报功能


## Version 2.0.4 (2020-2-5)
### Added:
- 支持单点登录功能，当且仅当用户传入指定相同resource且enableSSO为true时，启用单点登录功能


## Version 2.0.3 (2020-1-17)
### Optimized:
- jni采用动态注册
- 对System.loadLibrary进行异常捕获，来支持否是引入XMD库


## Version 2.0.2 (2020-1-15)
### Added:
- Java版本xmd替换为C++版本
### Deprecated:
- xmd-transceiver-1.0.2.31-SNAPSHOT.jar删除


## Version 1.4.2 (2019-12-05)
### Optimized:
- 优化RTS建立连接IP，Port选择问题


## Version 1.4.1 (2019-12-05)
### Added:
- 新增sendOnlineMessage接口，不存储历史消息，接收方离线，消息丢弃


## Version 1.3.7 (2019-11-01)
### Optimized:
- Backoff增加抖动系数来降低退避时间间隔的对齐概率
### Fixed:
- 修复pull()接口实现
### Added:
- 过滤onPullNotification通知时，也回复sequence ack


## Version 1.3.6 (2019-09-25)
### Fixed:
- 修复newInstance传入resource时，会生成两个token缓存文件


## Version 1.3.5 (2019-09-24)
### Added:
- 对缓存token信息的配置文件使用RC4加密
### Changed:
- 调整newInstance接口，将缓存路径细分为logCachePath和tokenCachePath


## Version 1.3.4 (2019-09-20)
### Fixed:
- 修复MIMCMessage、MIMCGroupMessage按sequence排序，重写compareTo导致的bug

## Version 1.3.3
### Added:
- 新增MIMCMessageHandler.onPullNotification()回调
- 调整发送单聊、群聊消息接口，支持isConversation


## Version 1.3.1
### Changed:
- 无线大群消息放在线程队列里处理


## Version 1.3.0
### Added:
- 新增消息队列处理线程，用于消息回调
- p2p消息，p2t消息放在消息队列里处理
- SerAck放在线程队列里处理
- 消息回调接口返回boolean类型，用于告知服务端消息是否被客户端正确接收


## Version 1.1.10
- 无限大群新增接口：void handleDismissUnlimitedGroup(long topicId)
- 消息长度上限调整为15k


## Version 1.2.8
### Added:
- new XMDTransceiver放到getXmdTransceiver中，需要时才去new


## Version 1.2.5
### Added:
- 无限大群新增接口：void handleDismissUnlimitedGroup(long topicId)
### Changed:
- 无限大群回调调整，handleCreateUnlimitedGroup，bool改为int


## Version 1.2.0
### Added:
- RTS引入Channel群聊，信令基本调通
