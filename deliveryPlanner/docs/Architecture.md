---ViewModel与Activity对象生命周期不同
    若Activity对象因为配置变更(旋转屏幕,切换深色模式、或者更改系统语言)被销毁,ViewModelStore 会保留 ViewModel 实例
此时ViewModel比Activity活得久(本质上系统认为这不是用户人为想退出应用)
    若用户退出了应用(back键或多任务列表移除该应用),代码会调用finish(),此时ViewModel会调用onCleared()被销毁

---内存泄漏
    何为内存泄漏:该对象该死却没死,就是内存泄漏
    如:无障碍服务生命周期比ViewModel长,(用户退出应用,进程不一定会死)
无障碍服务依然在,如果Service引用ViewModel实例,若用户退出应用,activity死了,viewmodel应该跟着死,但是gc发现其还被service引用,
那就不会回收这个对象,本该死的对象没死,会一直存在,并且viewmodel内部可能持有ui(context)引用,会一直占用内存,最后导致oom
    当用户点击强制停止,进程才会死,大家一起死

---每个组件要解耦:
    activity是菜单,给用户看的(提供ui供用户使用)
    viewModel是服务员,给客户提供菜单上的菜/记客户点的菜(负责给ui提供数据/或者获取ui传递来的数据)
    service是厨师,他要与viewmodel解耦,客人走了,菜单没用了,服务员也该下班了,厨师不能一直拉着服务员的手不让下班
    dao是仓库,厨师炒菜找仓库拿
    data是菜

---:架构铁律:下层组件（生命周期长的）绝对不能持有 上层组件（生命周期短的)。

---系统架构设计:MVVC
    Model: 数据层
    Controller: 控制层
    ViewModel: 数据交互层 
    view: UI层 (Activity)

--- Compose 的自动刷新机制:
    只要将属性定义为 Compose 的 MutableState 或 SnapshotStateList，那么,后台修改这些变量,在前台 UI会自动感知并重绘（Recompose）。
    使用 var xxx by mutableStateOf()，这样赋值时会自动通知UI

### todo list
    核心重构：确保 Service 和 UI 通过 GlobalState 和 Room 解耦，不要直接传对象引用。
    详情页解析：去抓取 UU 跑腿订单详情页的 Logcat 日志，写第二套解析逻辑（专门应付通知跳转的场景）。
    悬浮窗交互：在 Overlay 上增加 [导航] 和 [完成] 按钮，闭环“跑单-结束”的操作，让用户不需要切回主 App。
    容错机制：加上“抢单失败”的 Toast 监听回滚逻辑。


## 算法逻辑
    为了性能考虑,先判断顺不顺路,不做路径规划(可做批量订单判断)
    用户实际点了抢单,并且抢单成功,再做路径规划
    抢单失败,要回滚
    
    