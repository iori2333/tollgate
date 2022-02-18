# Tollgate

本项目为事件驱动的微服务系统设计演示之用。系统包括若干独立服务（SpringBoot应用）相互协同完成车辆的收费放行业务，其架构如下：

![](architecture.png)


### 原理


系统以`VehicleService`为中心，该服务对每辆进入收费站区域的车辆进行状态管理。
针对车辆出站的收费业务，我们采用 [SCXML](https://www.w3.org/TR/scxml/) 对车辆进行状态建模，并使用 [Apache Common SCXML](https://commons.apache.org/proper/commons-scxml/) 作为该模型的运行引擎。
前述架构中车辆状态图对应的SCXML如下。
```xml
<?xml version="1.0"?>
<scxml xmlns="http://www.w3.org/2005/07/scxml" version="1.0" initial="approached">

    <state id="approached">
        <transition event="start" target="recognizing"/>
    </state>

    <state id="recognizing">
        <transition event="recognition_fails" target="approached"/>
        <transition event="recognition_successes" target="recognized"/>
    </state>

    <state id="recognized">
        <transition event="validation_starts" target="validating"/>
    </state>

    <state id="validating">
        <transition event="validation_fails" target="recognized"/>
        <transition event="validation_successes." target="validated"/>
    </state>

    <state id="validated">
        <transition event="billing_starts" target="billing"/>
    </state>

    <state id="billing">
        <transition event="billing_fails" target="validated"/>
        <transition event="billing_successes" target="billed"/>
    </state>

    <state id="billed">
        <transition event="pay" target="paid"/>
    </state>

    <state id="paid">
        <transition event="leaving_detects" target="left"/>
    </state>

    <final id="left"/>

</scxml>
```
系统运行过程中`VehicleService`将车辆当前状态以消息形式通过消息中间件（RabbitMQ）发送给其他服务，其他服务订阅其需要的消息进行相应处理，处理结果再以消息形式发送给`VehicleService`驱动车辆状态发生变化，到达新状态时`VehicleService`再次发送消息通知其他服务，以此往复。


系统中各服务以消息驱动的方式协同运行，业务流程大体入下：

1. 车辆进入收费站（车道），假设存在一个`DetectingService`可感知这一过程（例如基于计算机视觉技术或通过RFID读取通行介质识别车辆到来），该服务发送一个`start`消息；
2. `start`消息由`VehicleService`为该车辆创建一个`VehicleStateMachine`对象，`VehicleService`再向该新创建的状态机对象转发`start`消息（此处存在一个特殊处理：因一辆新到车辆对应的状态机对象并不现成存在，所以本应驱动新来车辆状态进入正式业务阶段的`start`消息被用作触发状态机对象生成，然后再有`VehicleService`重发了一遍）；
3. 车辆状态机收到`start`消息后进入`recognizing`状态等待识别，进入该状态时`VehicleService`发出状态消息，该消息由`RecognizingService`接收，触发识别业务，并将识别结果保存在其内部；
4. 用户可通过向`RecognizingService`的Web接口发送请求查看识别结果（ http://localhost:8090/unconfirmed ），并确认识别结果正确（ http://localhost:8090/confirm?id=xxxxxx ）或确认识别错误（该接口暂未实现），相应地发送`recognition_successes`或`recognition_fails`消息；
5. `VehicleService`收到消息后驱动车辆状态发生变化，发送状态消息，以此往复。

### 消息中间件配置

待续...