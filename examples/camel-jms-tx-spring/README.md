Camel Transacted JMS Spring example
-----------------------------------

This example demonstrates using the camel-jms component with Spring and the WildFly Camel susbsystem to produce and consume JMS messages in a transacted session.

In this example, a Camel route consumes files from ${JBOSS_HOME}/standalone/data/orders and places their contents onto an in-memory ActiveMQ Artemis JMS queue
named 'OrdersQueue'. A second route consumes any messages from 'OrdersQueue', converts the message body to an 'Order' entity and persists it
to an in-memory database table named 'orders'.

If the order quantity is greater than 10, the Camel route throws an exception and the database / JMS transaction is rolled back.

CLI scripts take care of creating and removing the JMS 'OrdersQueue' for you when the
application is deployed and undeployed. These scripts are located within the `src/main/resources/cli` directory.

Prerequisites
-------------

* Maven
* An application server with the wildfly-camel subsystem installed

Running the example
-------------------

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`
3. When the WildFly server has started, browse to http://localhost:8080/example-camel-jms-tx-spring/orders

You should see a page titled 'Orders Received'. As we send orders to the example application, a list
of processed orders will be listed on this page.

Testing Camel Transacted JMS Spring
-----------------------------------

There are some example order XML files within the `src/main/resources/orders` directory.

* order-1.xml - Wireless keyboard, quantity 4
* order-2.xml - Wireless mouse, quantity 12
* order-3.xml - HDMI cable, quantity 1

To make Camel consume these files and send them to the 'OrdersQueue' JMS destination, simply copy them to the orders input
directory.

For Linux / Mac users:

    cp src/main/resources/orders/*.xml ${JBOSS_HOME}/standalone/data/orders/

For Windows users:

    copy src\main\resources\orders\*.xml %JBOSS_HOME%\standalone\data\orders\

Once the files have been consumed, you can return to http://localhost:8080/example-camel-jms-tx-spring/orders. You should see that only the wireless keyboard
and HDMI cable product orders were processed. What happened to the order contained within order-2.xml for the wireless mouse?

Look at the WildFly server console output. The output should show a sequence of events similar to the following.

```
[stdout] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Hibernate: insert into orders (productName, productSku, quantity, id) values (?, ?, ?, ?)
[route3] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Order quantity is greater than 10 - rolling back transaction!
[org.apache.camel.processor.DefaultErrorHandler] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Rollback (MessageId: queue_OrdersQueue_ID_8cc15741-cc92-11e4-8896-83967a57e23a on ExchangeId: ID-localhost-localdomain-51738-1426589044701-2-5) due: null
[stdout] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Hibernate: call next value for hibernate_sequence
[stdout] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Hibernate: insert into orders (productName, productSku, quantity, id) values (?, ?, ?, ?)
[route3] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Order processed successfully
[stdout] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Hibernate: call next value for hibernate_sequence
[stdout] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Hibernate: insert into orders (productName, productSku, quantity, id) values (?, ?, ?, ?)
[route3] (Camel (jms-camel-context) thread #1 - JmsConsumer[OrdersQueue]) Order processed successfully
```

Note the second line "Order quantity is greater than 10 - rolling back transaction!". Since the wireless mouse order requested a quantity greater than the
number permitted, the entire transaction was rolled back. No order was saved to the 'orders' database table and the JMS message was sent to the dead
letter queue, as specified by the RouteBuilder 'onException' policy. Hence it does not show up in the list of received orders.

Undeploy
--------

To undeploy the example run `mvn clean -Pdeploy`.
