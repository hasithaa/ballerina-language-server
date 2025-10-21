import ballerinax/rabbitmq;

listener rabbitmq:Listener rabbitmqListener = new ("localhost", 1231);

service "testqueue" on rabbitmqListener {
    remote function onMessage(rabbitmq:AnydataMessage message, rabbitmq:Caller caller) returns error? {
        do {

        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }
}
