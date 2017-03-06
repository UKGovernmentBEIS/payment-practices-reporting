/* Gradual disclosure */

function gradualDisclosure() {
    var panel1 = document.getElementById("show-if-payment-codes");
    var panel2 = document.getElementById("show-if-payment-changes");
    var panel3 = document.getElementById("show-if-payment-changes-notified");

    function showDependingOnValue(elem, name) {
        var radios = document.getElementsByName(name);
        var val = null;
        for (var i = 0; i < radios.length; i++) {
            if (radios[i].checked) {
                val = radios[i].value;
            }
        }

        if (val === "yes") {
            elem.style.display = "";
        } else {
            elem.style.display = "none";
        }
    }

    function subscribeToChange(elem, name) {
        var radios = document.getElementsByName(name);
        for (var i = 0; i < radios.length; i++) {
            radios[i].onclick = function () {
                showDependingOnValue(elem, name);
            }
        }
    }

    showDependingOnValue(panel1, "paymentCodes.yesNo");
    showDependingOnValue(panel2, "paymentTerms.paymentTermsChanged.changed.yesNo");
    showDependingOnValue(panel3, "paymentTerms.paymentTermsChanged.notified.yesNo");

    subscribeToChange(panel1, "paymentCodes.yesNo");
    subscribeToChange(panel2, "paymentTerms.paymentTermsChanged.changed.yesNo");
    subscribeToChange(panel3, "paymentTerms.paymentTermsChanged.notified.yesNo");
}