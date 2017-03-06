/* Validation */
function Validation(messages) {

    /**
     * See if the given node is an error message container (identified by having a class of "error-message").
     * If this node isn't an error message container then check its children recursively.
     * @param node - the root node to start searching for an error message container
     * @returns the first error message container found in the tree rooted in the provided node, or null if no
     * error message container was found
     */
    function findErrorMessageContainer(node) {
        if (!node) {
            return null;
        }
        return $(node).find(".error-message")[0];
    }

    function subscribe(obj, eventname, callback) {
        var old = obj[eventname];
        obj[eventname] = function (x) {
            if (old) old(x);
            return callback(x);
        };
    }

    function validateTextInput(id, validate) {
        var e = document.getElementById(id);
        if (!e) {
            return;
        }

        var formGroup = e.parentElement;
        var messageContainer = findErrorMessageContainer(formGroup);

        if (!messageContainer) {
            return;
        }

        var callbackClear = function () {
            messageContainer.innerHTML = "&nbsp;";
            formGroup.className = "form-group";
        };

        subscribe(e, "onblur", function () {
            if (e.value === "") {
                return;
            }
            var invalidation = validate(e.value);
            if (invalidation) {
                messageContainer.innerHTML = invalidation;
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

        var message = findErrorMessageContainer(container);

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
        var year = document.getElementById(namePrefix + ".year");
        var month = document.getElementById(namePrefix + ".month");
        var day = document.getElementById(namePrefix + ".day");
        if (!year || !month || !day) {
            return;
        }

        var message = findErrorMessageContainer(year.parentElement.parentElement);

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

    function asInteger(text) {
        var i = parseInt(text)
        if (isNaN(i)) {
            return null;
        } else {
            return i
        }
    }

    function dateValid(year, month, day) {
        var date = new Date(asInteger(year), asInteger(month) - 1, asInteger(day), 0, 0, 0, 0);
        return (!date.getFullYear() || date.getFullYear() !== asInteger(year)
            || date.getMonth() !== asInteger(month) - 1
            || date.getDate() !== asInteger(day)) && messages.date;
    }

    function dateFuture(year, month, day) {
        var date = new Date(asInteger(year), asInteger(month) - 1, asInteger(day), 0, 0, 0, 0);
        return new Date().getTime() < date.getTime() && messages.future;
    }

    function textNonNegative(text) {
        return asInteger(text) < 0 && messages.nonnegative;
    }

    function textInteger(text) {
        return asInteger(text) === null && messages.integer;
    }

    function textPercentageBounds(text) {
        return (asInteger(text) < 0 || asInteger(text) > 100) && messages.percentagebounds;
    }

    function textPositiveInteger(x) {
        return textInteger(x) || textNonNegative(x);
    }

    function textPercentage(x) {
        return textPercentageBounds(x) || textInteger(x);
    }

    function multiStartBeforeEnd(inputs) {
        var startYear = inputs[0], startMonth = inputs[1], startDay = inputs[2],
            endYear = inputs[3], endMonth = inputs[4], endDay = inputs[5];

        if (dateValid(startYear, startMonth, startDay) || dateFuture(startYear, startMonth, startDay) || dateValid(endYear, endMonth, endDay) || dateFuture(endYear, endMonth, endDay)) {
            return false;
        } else {
            var start = new Date(asInteger(startYear), asInteger(startMonth) - 1, asInteger(startDay), 0, 0, 0, 0);
            var end = new Date(asInteger(endYear), asInteger(endMonth) - 1, asInteger(endDay), 0, 0, 0, 0);
            return start.getTime() > end.getTime() && messages.startbeforeend;
        }
    }

    /**
     * Check that the three inputs sum to 100 +/- 2 (to allow for rounding errors because the numbers should be integers)
     * @param values - three integers
     * @returns an error message if the sum is less than 98 or more than 102
     */
    function multiSumTo100(values) {
        if (textPercentage(values[0]) || textPercentage(values[1]) || textPercentage(values[2])) {
            return false;
        } else {
            var sum = asInteger(values[0]) + asInteger(values[1]) + asInteger(values[2]);
            return (sum > 102 || sum < 98) && messages.sumto100;
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

function validationPlumbing(messages) {
    var v = new Validation(messages);

    v.validateDateInput("reportDates.startDate", v.validations.dateValid);
    v.validateDateInput("reportDates.endDate", v.validations.dateValid);

    v.validateMultiple(["reportDates.startDate.year", "reportDates.startDate.month", "reportDates.startDate.day",
            "reportDates.endDate.year", "reportDates.endDate.month", "reportDates.endDate.day"],
        document.getElementById("reportDates.endDate.year").parentElement.parentElement,
        v.validations.multiStartBeforeEnd);

    v.validateTextInput("paymentHistory.averageDaysToPay", v.validations.textPositiveInteger);
    v.validateTextInput("paymentHistory.percentPaidBeyondAgreedTerms", v.validations.textPercentage);
    v.validateTextInput("paymentHistory.percentageSplit.percentWithin30Days", v.validations.textPercentage);
    v.validateTextInput("paymentHistory.percentageSplit.percentWithin60Days", v.validations.textPercentage);
    v.validateTextInput("paymentHistory.percentageSplit.percentBeyond60Days", v.validations.textPercentage);

    v.validateMultiple([
            "paymentHistory.percentageSplit.percentWithin30Days",
            "paymentHistory.percentageSplit.percentWithin60Days",
            "paymentHistory.percentageSplit.percentBeyond60Days"
        ],
        document.getElementsByName("paymentHistory.percentageSplit.percentWithin30Days")[0].parentElement.parentElement,
        v.validations.multiSumTo100);
}
