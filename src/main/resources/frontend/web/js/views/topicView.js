App.TopicView = Em.View.extend({
    tagName : 'div',
    elementId: 'topic',
    contentBinding: 'controller.content',

    throughputPerSec : 0,
    subscribersNumber : 0,
    subTopicsNumber : 0,

    listenStats : function(series, graph) {   
     	var view = this;
        App.Dao.get("eventBus").onValue(function(stats) {
            var throughputPerSec = stats.throughputPerSec;
            var subscribersNumber = stats.subscribersNumber;
            var subTopicsNumber = stats.subTopicsNumber;

            view.set('throughputPerSec',throughputPerSec);
            view.set('subscribersNumber',subscribersNumber);
            view.set('subTopicsNumber',subTopicsNumber);
           
            var xNow = moment().unix();
            series[0].push({x: xNow, y:throughputPerSec});
            series[1].push({x: xNow, y:subscribersNumber});
            series[2].push({x: xNow, y:subTopicsNumber});

            graph.update();
        });
    },

    didInsertElement: function() {
        var view = this;
        var seriesData = [ [], [], [] ];
        seriesData.forEach(function(series) {
            series.push( {x: moment().unix(), y: NaN} );
        });
        
        var palette = new Rickshaw.Color.Palette( { scheme: 'munin' } );
        var graph = new Rickshaw.Graph( {
            element: document.getElementById("chart"),
            width: view.calculateFitWidth(),
            height: view.calculateFitHeight(),
            renderer: 'line',
            interpolation: 'linear',
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
                    name: 'Throughput / sec'
                }, {
                    color: palette.color(),
                    data: seriesData[1],
                    name: 'Subscriber'
                },
                {
                    color: palette.color(),
                    data: seriesData[2],
                    name: 'Subtopic'
                }
            ]
        } );

        view.listenStats(seriesData, graph);
        view.graphExtensions(graph);
    },

    calculateFitWidth : function()  {
        return $(window).width() - $("#chart").offset().left - 80;
    },

    calculateFitHeight : function() {
        return $(window).height() - $("#chart").offset().top - 150;
    },

    graphExtensions : function (graph) {
        graph.render();

        var preview = new Rickshaw.Graph.RangeSlider.Preview( {
            graph: graph,
            element: document.getElementById('preview'),
        });

        var previewXAxis = new Rickshaw.Graph.Axis.Time({
            graph: preview.previews[0],
            timeFixture: new Rickshaw.Fixtures.Time.Local(),
            ticksTreatment: ticksTreatment
        });
        previewXAxis.render();

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
        
        //https://github.com/shutterstock/rickshaw/issues/364
        graph.configure({renderer:'line'});
        graph.render;
    }        
});