# Android Google Cloud Messaging Example

> Google Cloud Messaging (GCM) is a service that enables developers to send data from servers to both Android applications or Chrome apps and extensions

This Android project shows how to implement GCM. This example allows you to send a request to a test server, that will then send a GCM Message to the same device, and it will display a notification.

You need to add a project ID and API keys (obtained from Google APIs console) on MainActivity.java for this example to work:

    /**
     * YOU NEED TO CHANGE THIS VALUE WITH THE
     * PROJECT ID YOU GOT ON GOOGLE APIS CONSOLE
     */
    String SENDER_ID = "";

    /**
     * YOU NEED TO CHANGE THIS VALUE WITH THE
     * API SERVER KEY YOU GOT ON GOOGLE APIS CONSOLE
     *
     * NB: do not forget to allow any IP addresses for this test
     */
    String GOOGLE_API_KEY = "";
    
Once you've done it, run the application and press the "Request" button, you should then receive a notification showing "Hello GCM world"

*Please note: the test server (https://pablophg-gcm-test.herokuapp.com/index.php) is hosted on a free Heroku instance, therefore when it's been inactive for a long time, it may need a longer time to start and load the very first request you make.*
