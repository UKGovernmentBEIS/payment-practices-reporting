/* Gradual disclosure */

function gradualDisclosure() {
    var data =[
        ["show-if-payment-codes", "paymentCodes.yesNo"],
        ["show-if-payment-changes", "paymentTerms.paymentTermsChanged.changed.yesNo"],
        ["show-if-payment-changes-notified", "paymentTerms.paymentTermsChanged.notified.yesNo"]
    ];

    function showPanelIfYes(panelId, checkboxName) {
        var panel = document.getElementById(panelId);
        var radios = document.getElementsByName(checkboxName);
        var val = "no";

        for (var i = 0; i < radios.length; i++) {
            if (radios[i].checked) {
                val = radios[i].value;
            }
        }

        if (val === "yes") {
            panel.style.display = "";
        } else {
            panel.style.display = "none";
        }
    }

    function subscribeToChange(panelId, checkboxName) {
        var radios = document.getElementsByName(checkboxName);
        for (var i = 0; i < radios.length; i++) {
            radios[i].onclick = function () {
                showPanelIfYes(panelId, checkboxName);
            }
        }
    }

    for (var j = 0; j < data.length; j++) {
        showPanelIfYes(data[j][0], data[j][1]);
        subscribeToChange(data[j][0], data[j][1]);
    }
}