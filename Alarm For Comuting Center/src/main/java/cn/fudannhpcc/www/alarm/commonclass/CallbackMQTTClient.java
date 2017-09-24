package cn.fudannhpcc.www.alarm.commonclass;

public class CallbackMQTTClient {

    public CallbackMQTTClient(IMQTTMessageReceiver messageReceiver) {
//        imqttMessageReceiver = messageReceiver;
//        mqtt = new MQTT();
//        mqtt.setKeepAlive((short) 120);
//        mqtt.setReconnectDelay(1000);
    }

    public interface IMQTTMessageReceiver {
//        void onReceiveMQTTMessage(String topic, Buffer payload);
    }
}
