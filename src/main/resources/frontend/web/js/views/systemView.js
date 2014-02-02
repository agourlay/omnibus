App.SystemView = Em.View.extend({
    tagName : 'div',
    elementId: 'system',

    totalRequests : 0,
    openRequests : 0,
    maxOpenRequests : 0,

    totalConnections : 0,
    openConnections : 0,
    maxOpenConnections : 0,

    requestTimeouts : 0,
    uptime : 0,

     listenStats : function(series, graph) {   
     	var view = this;
    	var source = new EventSource("stats/system?mode=streaming");
        source.addEventListener('message', function(e) {
            var stats = $.parseJSON(e.data);
            var totalRequests = stats.totalRequests;
            var openRequests = stats.openRequests;
            var maxOpenRequests = stats.maxOpenRequests;
            var totalConnections = stats.totalConnections;
            var openConnections = stats.openConnections;
            var maxOpenConnections = stats.maxOpenConnections;
            var requestTimeouts = stats.requestTimeouts;

            var xNow = moment().unix();
            series[0].push({x: xNow, y:openRequests});
            series[1].push({x: xNow, y:openConnections});

            view.set('totalRequests',totalRequests);
            view.set('openRequests',openRequests);
            view.set('maxOpenRequests',maxOpenRequests);
            view.set('totalConnections',totalConnections);
            view.set('openConnections',openConnections);
            view.set('maxOpenConnections',maxOpenConnections);
            view.set('requestTimeouts',requestTimeouts);
            view.set('uptime',moment.duration(stats.uptimeInMilli).humanize());
            graph.update();
        }, false);
    },

    didInsertElement: function() {
        var view = this;
        var seriesData = [ [], [], [] ];
        seriesData.forEach(function(series) {
            series.push( {x: moment().unix(), y: NaN} );
        });
        
        var palette = new Rickshaw.Color.Palette( { scheme: 'colorwheel' } );
        var graph = new Rickshaw.Graph( {
            element: document.getElementById("chart"),
            width: calculateFitWidth(),
            height: calculateFitHeight(),
            renderer: 'line',
            padding : {
                top : 0.05,
                bottom : 0.05
            },
            stroke: true,
            preserve: true,
            series: [
                {
                    color: palette.color(),
                    data: seriesData[0],
                    name: 'Opened requests'
                }, {
                    color: palette.color(),
                    data: seriesData[1],
                    name: 'Opened connections'
                }
            ]
        } );

        view.listenStats(seriesData, graph);
        graphExtensions(graph);
    }        
});