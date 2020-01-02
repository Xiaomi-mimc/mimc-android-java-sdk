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
