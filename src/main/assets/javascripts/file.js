/* Validation */
function Validation() {
    function findErrorMessage(parent) {
        if (!parent) return null;
        if (parent.className && parent.className.indexOf("error-message") !== -1) {
            return parent;
        }

        for (var i = 0; i < parent.childNodes.length; i++) {
            var found = findErrorMessage(parent.childNodes[i]);
            if (found) {
                return found;
            }
        }
        return null;
    }

    function subscribe(obj, eventname, callback) {
        var old = obj[eventname];
        obj[eventname] = function (x) {
            if (old) old(x);
            return callback(x);
        };
    }

    function validateTextInput(name, validate) {
        var allElements = document.getElementsByName(name);
        if (!allElements || allElements.length <= 0) {
            return;
        }

        var e = allElements[0];
        var formGroup = e.parentElement;
        var message = findErrorMessage(formGroup);

        if (!message) {
            return;
        }

        var callbackClear = function () {
            message.innerHTML = "&nbsp;";
            formGroup.className = "form-group";
        };

        subscribe(e, "onblur", function () {
            if (e.value === "") {
                return;
            }
            var invalidation = validate(e.value);
            if (invalidation) {
                message.innerHTML = invalidation;
                formGroup.className = "form-group error";
            }
        });
        subscribe(e, "onkeydown", callbackClear)
    }

    function validateMultiple(names, container, validate) {
        var elements = [];
        for (var i = 0; i < names.length; i++) {
            var es = document.getElementsByName(names[i]);
            if (!es || es.length < 1) {
                return;
            }
            elements.push(es[0]);
        }

        var message = findErrorMessage(container);

        for (var i2 = 0; i2 < elements.length; i2++) {
            var element = elements[i2];
            subscribe(element, "onblur", function () {
                var values = [];
                for (var j = 0; j < elements.length; j++) {
                    if (elements[j].value === "") {
                        return true;
                    }
                    values.push(elements[j].value);
                }
                var invalidation = validate(values);
                if (invalidation) {
                    message.innerHTML = invalidation;
                    message.parentElement.parentElement.className = "form-group error";
                }
                return true;
            });
            subscribe(element, "onkeydown", function () {
                message.innerHTML = "&nbsp;";
                message.parentElement.parentElement.className = "form-group";
            });
        }
    }

    function validateDateInput(namePrefix, validate) {
        var years = document.getElementsByName(namePrefix + "year");
        var months = document.getElementsByName(namePrefix + "month");
        var days = document.getElementsByName(namePrefix + "day");
        if (!years || !years[0] || !months || !months[0] || !days || !days[0]) {
            return;
        }

        var year = years[0];
        var month = months[0];
        var day = days[0];

        var message = findErrorMessage(year.parentElement.parentElement);

        var callbackClear = function () {
            message.innerHTML = "&nbsp;";
            message.parentElement.parentElement.className = "form-group";
        };

        var callback = function () {
            if (year.value === "" || month.value === "" || day.value === "") {
                return;
            }
            var invalidation = validate(year.value, month.value, day.value);
            if (invalidation) {
                message.innerHTML = invalidation;
                message.parentElement.parentElement.className = "form-group error";
            }
        };

        subscribe(year, "onblur", callback);
        subscribe(month, "onblur", callback);
        subscribe(day, "onblur", callback);

        subscribe(year, "onkeydown", callbackClear);
        subscribe(month, "onkeydown", callbackClear);
        subscribe(day, "onkeydown", callbackClear)
    }

    function isSetTrue(name) {
        var e = document.getElementsByName(name);
        if (!e) return false;
        for (var i = 0; i < e.length; i++) {
            if (e[i].checked && e[i].value === "1") {
                return true;
            }
        }
        return false;
    }

    var messages = {
        integer: "Please round up or down to the nearest whole number",
        nonnegative: "This should be a non-negative number",
        percentagebounds: "This should be a number between 0 and 100",
        sumto100: "Figures A, B and C do not add up to 100",
        date: "This date is invalid",
        future: "Reporting period cannot cover the future",
        startbeforeend: "The end date cannot be before the start date"
    };

    function asInteger(text) {
        var trimmed = text.replace(/^\s+|\s+$/gm, "");
        var match = /^(-?[0-9]+)(\.0+)?[^0-9]*$/.exec(trimmed);
        return match ? parseInt(match[1]) : null;
    }

    function asNumber(text) {
        var trimmed = text.replace(/^\s+|\s+$/gm, "");
        var match = /^(-?[0-9]+(\.[0-9]+)?)[^0-9]*$/.exec(trimmed);
        return match ? parseInt(match[1]) : null;
    }

    function dateValid(year, month, day) {
        var date = new Date(asInteger(year), asInteger(month) - 1, asInteger(day), 0, 0, 0, 0);
        return (!date.getFullYear() || date.getFullYear() !== asInteger(year)
            || date.getMonth() !== asInteger(month) - 1
            || date.getDate() !== asInteger(day)) && messages.date;
    }

    function dateFuture(year, month, day) {
        return new Date().getTime() < new Date(asInteger(year), asInteger(month), asInteger(day), 0, 0, 0, 0).getTime() && messages.future;
    }

    function textNonNegative(text) {
        return asNumber(text) < 0 && messages.nonnegative;
    }

    function textInteger(text) {
        return asInteger(text) === null && messages.integer;
    }

    function textPercentageBounds(text) {
        return (asNumber(text) < 0 || asNumber(text) > 100) && messages.percentagebounds;
    }

    function textPositiveInteger(x) {
        return textNonNegative(x) || textInteger(x);
    }

    function textPercentage(x) {
        return textPercentageBounds(x) || textInteger(x);
    }

    function multiStartBeforeEnd(inputs) {
        var startYear = inputs[0], startMonth = inputs[1], startDay = inputs[2],
            year = inputs[3], month = inputs[4], day = inputs[5];

        if (dateValid(startYear, startMonth, startDay) || dateFuture(startYear, startMonth, startDay) || dateValid(year, month, day) || dateFuture(year, month, day)) {
            return false;
        } else {
            var start = new Date(asInteger(startYear), asInteger(startMonth) - 1, asInteger(startDay), 0, 0, 0, 0);
            var end = new Date(asInteger(year), asInteger(month) - 1, asInteger(day), 0, 0, 0, 0);
            return start.getTime() > end.getTime() && messages.startbeforeend;
        }
    }

    function multiSumTo100(x) {
        if (textPercentage(x[0]) || textPercentage(x[1]) || textPercentage(x[2])) {
            return false;
        } else {
            var numberOne = asInteger(x[0]), numberTwo = asInteger(x[1]), numberThree = asInteger(x[2]);
            return (numberOne + numberTwo + numberThree > 102 || numberOne + numberTwo + numberThree < 98) && messages.sumto100;
        }
    }

    this.validateTextInput = validateTextInput;
    this.validateMultiple = validateMultiple;
    this.validateDateInput = validateDateInput;

    this.validations = {};

    this.validations.dateValid = function (y, m, d) {
        return dateValid(y, m, d) || dateFuture(y, m, d);
    };

    this.validations.textPositiveInteger = textPositiveInteger;
    this.validations.textPercentage = textPercentage;
    this.validations.multiSumTo100 = multiSumTo100;
    this.validations.multiStartBeforeEnd = multiStartBeforeEnd;
}

function validationPlumbing() {
    var v = new Validation();

    v.validateDateInput("StartDate_", v.validations.dateValid);
    v.validateDateInput("EndDate_", v.validations.dateValid);

    v.validateMultiple(["StartDate_year", "StartDate_month", "StartDate_day", "EndDate_year", "EndDate_month", "EndDate_day"],
        document.getElementsByName("EndDate_year")[0].parentElement.parentElement,
        v.validations.multiStartBeforeEnd);

    v.validateTextInput("AverageTimeToPay", v.validations.textPositiveInteger);
    v.validateTextInput("PercentInvoicesPaidBeyondAgreedTerms", v.validations.textPercentage);
    v.validateTextInput("PercentInvoicesWithin30Days", v.validations.textPercentage);
    v.validateTextInput("PercentInvoicesWithin60Days", v.validations.textPercentage);
    v.validateTextInput("PercentInvoicesBeyond60Days", v.validations.textPercentage);

    v.validateMultiple(["PercentInvoicesWithin30Days", "PercentInvoicesWithin60Days", "PercentInvoicesBeyond60Days"],
        document.getElementsByName("PercentInvoicesWithin30Days")[0].parentElement.parentElement,
        v.validations.multiSumTo100);
}
