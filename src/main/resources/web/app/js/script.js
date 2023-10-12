import $ from "jquery"
import "patternfly/dist/css/patternfly.css";
import "patternfly/dist/css/patternfly-additions.css";
import "datatables.net";
import "drmonty-datatables-colvis";
import "datatables.net-colreorder";
import "bootstrap-switch";
import "jquery-match-height";

window.jQuery = $;

require("bootstrap");
require("patternfly/dist/js/patternfly.js");
require("bootstrap-touchspin")

$(document).ready(function () {

    // matchHeight the contents of each .card-pf and then the .card-pf itself
    $(".row-cards-pf > [class*='col'] > .card-pf .card-pf-title").matchHeight();
    $(".row-cards-pf > [class*='col'] > .card-pf > .card-pf-body").matchHeight();
    $(".row-cards-pf > [class*='col'] > .card-pf > .card-pf-footer").matchHeight();
    $(".row-cards-pf > [class*='col'] > .card-pf").matchHeight();

    // Initialize the vertical navigation
    $().setupVerticalNavigation(true);
});

