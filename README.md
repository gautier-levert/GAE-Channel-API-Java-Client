GAE Channel API Java Client
===========================

This is a simple Java implementation of Channel API functionnality of Google App Engine.

From Google documentation :
---------------------------

>The Channel API creates a persistent connection between an application and its users, allowing the application to send real time messages without the use of polling.
>
>To use the API, you must add a JavaScript client in your web pages. The client performs these tasks:
> - Connecting to the channel once it receives the channel’s unique token from the server
> - Listening on the channel for updates regarding other clients and making appropriate use of the data (e.g. updating the interface, etc.)
> - Sending update messages to the server so they may be passed on to remote clients
>
>Your application, acting as server, is responsible for:
> - Creating a unique channel for individual JavaScript clients
> - Creating and sending a unique token to each JavaScript client so they may connect and listen to their channel
> - Receiving update messages from clients via HTTP requests
> - Sending update messages to clients via their channels
> - Optionally, managing client connection state.

By default there is only a JavaScript API for client side: https://cloud.google.com/appengine/docs/java/channel/javascript

**To be honest, I just arranged GVSU MASL version: https://github.com/gvsumasl/jacc**