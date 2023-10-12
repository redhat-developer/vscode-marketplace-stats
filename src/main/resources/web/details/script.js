import '../app/js/script.js'
import $ from 'jquery'
import Highcharts from 'highcharts'

function toPoint(data) {
    return [
        new Date(data.time).getTime(),
        data.installs? data.installs : data.total_installed
    ];
}

var promiseCache = {};
function fetchAndUpdateContent(extensionName, tabId) {
    if (!promiseCache[tabId]) {
        // Create a new promise if it doesn't exist in the cache
        promiseCache[tabId] = fetchDataTab(extensionName, tabId)
            .then(function (data) {
                // Update the tab content with the fetched data
                renderChart(tabId, data);
            })
    }
}

async function setupCharts(extensionName) {
    Highcharts.setOptions({
        global: {
            useUTC: true
        }
    });
    // Event handler for tab clicks
    $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        var tab = e.target.getAttribute('data-tab');
        fetchAndUpdateContent(extensionName, tab);
    });

    // Fetch and update the content for the active tab when the page loads
    var activeTab = $('.nav-tabs .active a').data('tab');
    fetchAndUpdateContent(extensionName, activeTab);
};

function renderChart(tabId, series) {
    var title = $(tabId).attr('data-title')
    var chart = new Highcharts.Chart({
        chart: {
            renderTo: tabId+"-container",
            //type: 'spline',
            zoomType: 'x',
            resetZoomButton: {
                position: {
                    align: 'left',
                    verticalAlign: 'top', // by default
                    x: 10,
                    y: 10
                },
                relativeTo: 'chart'
            },
            animation: Highcharts.svg, // don't animate in old IE
            marginRight: 10,
        },
        title: {
            text: title//'redhat.java installations'
        },
        xAxis: {
            type: 'datetime'//,
        },
        yAxis: {
            title: {
                text: title
            },
            floor: 0,
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
        legend: {
            enabled: false
        },
        exporting: {
            enabled: false
        },
        plotOptions: {
            area: {
                fillColor: {
                    linearGradient: {
                        x1: 0,
                        y1: 0,
                        x2: 0,
                        y2: 1
                    },
                    stops: [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                    ]
                },
                marker: {
                    radius: 2
                },
                lineWidth: 1,
                states: {
                    hover: {
                        lineWidth: 1
                    }
                },
                threshold: null
            }
        },
        series: series
    });
}

async function fetchDataTab(extensionName, tabId) {
    var start = Date.now();
    var response = await fetch('/stats/'+extensionName+'?stats=time&stats='+tabId);
    var versions = JSON.parse(await response.text());
    var i = 0;
    var data = null;
    var point = null;
    var latest = null;
    var series = [];
    var startVersion = 0;
    var stopVersion = 0;
    var elapsedVersion = 0;
    for (var l = versions.length; i < l; i++) {
        startVersion = Date.now();
        var version = versions[i];
        data = [];
        for (var z = version.events.length, j = 0; j < z; j++) {
            point = toPoint(version.events[j]);
            data.push(point);
            if (latest == null || latest[0] < point[0]) {
                latest = point;
            }
        }
        var serie = {
            name: version._id,
            data: data
        }
        series.push(serie);
        stopVersion = Date.now();
        elapsedVersion = stopVersion - startVersion;
        //chart.redraw();
        console.log("Loaded " + serie.name + " in "+elapsedVersion+"ms");
    }
    if (latest !== null) {
        var label = $("#"+tabId).attr("data-title")
        $("#"+tabId+"-value").html(format(latest[1])+" "+label);
    }
    var elapsed = new Date().getTime() - start;
    console.log("Loaded " + i + " data points for "+tabId+" in " + elapsed + "ms");
    return series;
}

function format(value) {
    return value.toLocaleString(
        undefined, // use a string like 'en-US' to override browser locale
    );
}

window.setupCharts = setupCharts;