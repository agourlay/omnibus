function calculateFitWidth ()  {
    return $(window).width() - $("#chart").offset().left - 70;
}

function calculateFitHeight () {
    return $(window).height() - $("#chart").offset().top - 110;
}

function graphExtensions(graph) {
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