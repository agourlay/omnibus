App.System = Em.Object.extend({
    totalRequests : 0,
    openRequests : 0,
    maxOpenRequests : 0,
    totalConnections : 0,
    openConnections : 0,
    maxOpenConnections : 0,
    requestTimeouts : 0,
    uptime : 0
});