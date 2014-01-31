App.SystemView = Em.View.extend({
    tagName : 'div',
    elementId: 'system',

    totalRequests : 0,
    openRequests : 0,
    maxOpenRequests : 0,
    totalConnections : 0,
    totalConnections : 0,
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

    calculateFitWidth : function() {
        return $(window).width() - $("#chart").offset().left - 70;
    },

    calculateFitHeight : function() {
        return $(window).height() - $("#chart").offset().top - 60;
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
            width: view.calculateFitWidth(),
            height: view.calculateFitHeight(),
            renderer: 'multi',
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
                    renderer: 'line',
                    name: 'Opened requests'
                }, {
                    color: palette.color(),
                    data: seriesData[1],
                    renderer: 'line',
                    name: 'Opened connections'
                }
            ]
        } );

        view.listenStats(seriesData, graph);
        graph.render();

        var hoverDetail = new Rickshaw.Graph.HoverDetail( {
            graph: graph
        } );

        var annotator = new Rickshaw.Graph.Annotate( {
            graph: graph,
            element: document.getElementById('timeline')
        } );

        var legend = new Rickshaw.Graph.Legend( {
            graph: graph,
            element: document.getElementById('legend')

        } );

        var order = new Rickshaw.Graph.Behavior.Series.Order( {
            graph: graph,
            legend: legend
        } );

        var highlighter = new Rickshaw.Graph.Behavior.Series.Highlight( {
            graph: graph,
            legend: legend
        } );

        var ticksTreatment = 'glow';

        var xAxis = new Rickshaw.Graph.Axis.Time( {
            graph: graph,
            ticksTreatment: ticksTreatment
        } );

        xAxis.render();

        var yAxis = new Rickshaw.Graph.Axis.Y( {
            graph: graph,
            tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
            ticksTreatment: ticksTreatment
        } );

        yAxis.render();
    }        
});