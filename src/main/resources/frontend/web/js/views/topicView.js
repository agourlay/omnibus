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
            width: calculateFitWidth(),
            height: calculateFitHeight(),
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
        graphExtensions(graph);
    }        
});