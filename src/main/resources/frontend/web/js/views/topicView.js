App.TopicView = Em.View.extend({
    tagName : 'div',
    elementId: 'topic',
    contentBinding: 'controller.content',

     listenStats : function(series, graph) {   
     	var view = this;
    	var source = new EventSource("stats/topics/"+view.content+"?mode=streaming");
        source.addEventListener('message', function(e) {
            var stats = $.parseJSON(e.data);

            var throughputPerSec = stats.throughputPerSec;
            var subscribersNumber = stats.subscribersNumber;
            var subTopicsNumber = stats.subTopicsNumber;
           
            var xNow = moment().unix();
            series[0].push({x: xNow, y:throughputPerSec});
            series[1].push({x: xNow, y:subscribersNumber});
            series[2].push({x: xNow, y:subTopicsNumber});

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
                    renderer: 'area',
                    name: 'Throughput per sec'
                }, {
                    color: palette.color(),
                    data: seriesData[1],
                    renderer: 'line',
                    name: 'Subscriber number'
                },
                {
                    color: palette.color(),
                    data: seriesData[2],
                    renderer: 'line',
                    name: 'Sub-topic number'
                }
            ]
        } );

        view.listenStats(seriesData, graph);
        graph.render();

        var slider = new Rickshaw.Graph.RangeSlider( {
            graph: graph,
            element: document.getElementById('slider')
        } );

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