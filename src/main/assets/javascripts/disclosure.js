/* Gradual disclosure */

function gradualDisclosure() {
    var data = [
        ["show-if-payment-codes-short", "paymentCodes.yesNo"],
        ["show-if-payment-codes-long", "otherInformation.paymentCodes.yesNo"],
        ["show-if-payment-changes", "paymentTerms.paymentTermsChanged.changed.yesNo"],
        ["show-if-payment-changes-notified", "paymentTerms.paymentTermsChanged.notified.yesNo"]
    ];

    function showPanelIfYes(panelId, checkboxName) {
        var panel = document.getElementById(panelId);
        var radios = document.getElementsByName(checkboxName);
        var yesNo = "no";

        for (var i = 0; i < radios.length; i++) {
            if (radios[i].checked) {
                yesNo = radios[i].value;
            }
        }

        panel.style.display = yesNo === "yes" ? "" : "none";
    }

    function subscribeToChange(panelId, checkboxName) {
        var radios = document.getElementsByName(checkboxName);
        for (var i = 0; i < radios.length; i++) {
            radios[i].onclick = function () {
                showPanelIfYes(panelId, checkboxName);
            };
        }
    }

    for (var j = 0; j < data.length; j++) {
        var panelId = data[j][0];
        var checkboxName = data[j][1];

        var panel = document.getElementById(panelId);
        if (panel !== null) {
            showPanelIfYes(panelId, checkboxName);
            subscribeToChange(panelId, checkboxName);
        }
    }
}