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
        return $(node).find(".error-message")[0];
    }

    function subscribe(element, eventname, callback) {
        var old = element[eventname];
        element[eventname] = function (x) {
            if (old) {
                old(x);
            }
            return callback(x);
        };
    }

    function validateTextInput(id, validationFunction) {
        var e = document.getElementById(id);

        var formGroup = $(e).parents(".form-group").first();
        var messageContainer = formGroup.find(".error-message");

        var clearError = function () {
            messageContainer.html("&nbsp;");
            formGroup.removeClass("error");
        };

        subscribe(e, "onblur", function () {
            if (e.value === "") {
                return;
            }
            var errorMessage = validationFunction(e);
            if (errorMessage) {
                messageContainer.html(errorMessage);
                formGroup.addClass("error");
            }
        });
        subscribe(e, "onkeydown", clearError);
    }

    function validateMultiple(names, validate) {
        var elements = [];
        for (var nameIndex = 0; nameIndex < names.length; nameIndex++) {
            var es = document.getElementsByName(names[nameIndex]);
            if (!es || es.length < 1) {
                return;
            }
            elements.push(es[0]);
        }

        var formGroup = $(elements[0]).parents(".form-group").last();
        var messageContainer = formGroup.find(".error-message").first();

        for (var elementIndex = 0; elementIndex < elements.length; elementIndex++) {
            var element = elements[elementIndex];

            subscribe(element, "onblur", function () {
                var values = [];
                for (var j = 0; j < elements.length; j++) {
                    if (elements[j].value === "") {
                        return true;
                    }
                    values.push(elements[j]);
                }
                var errorMessage = validate(values);
                if (errorMessage) {
                    messageContainer.html(errorMessage);
                    formGroup.addClass("error");
                }
                return true;
            });
            subscribe(element, "onkeydown", function () {
                messageContainer.html("&nbsp;");
                formGroup.removeClass("error");
            });
        }
    }

    function validateDateInput(idPrefix, validationFunction) {
        var year = document.getElementById(idPrefix + ".year");
        var month = document.getElementById(idPrefix + ".month");
        var day = document.getElementById(idPrefix + ".day");
        if (!year || !month || !day) {
            return;
        }

        var formGroup = $(year).parents(".form-group").first();
        var messageContainer = formGroup.find(".error-message").first();

        var clearError = function () {
            messageContainer.html("&nbsp;");
            formGroup.removeClass("error");
        };

        var validationCallback = function () {
            if (year.value === "" || month.value === "" || day.value === "") {
                return;
            }
            var errorMessage = validationFunction(year, month, day);
            if (errorMessage) {
                messageContainer.html(errorMessage)
                formGroup.addClass("error");
            }
        };

        subscribe(year, "onblur", validationCallback);
        subscribe(month, "onblur", validationCallback);
        subscribe(day, "onblur", validationCallback);

        subscribe(year, "onkeydown", clearError);
        subscribe(month, "onkeydown", clearError);
        subscribe(day, "onkeydown", clearError);
    }

    function asInteger(element) {
        var text = element.value
        var i = parseInt(text);
        if (isNaN(i)) {
            return null;
        } else {
            return i;
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

    function textNonNegative(e) {
        return asInteger(e) < 0 && messages.nonnegative;
    }

    function textInteger(e) {
        return asInteger(e) === null && messages.integer;
    }

    function textPercentageBounds(e) {
        return (asInteger(e) < 0 || asInteger(e) > 100) && messages.percentagebounds;
    }

    function validateTextPositiveInteger(e) {
        return textInteger(e) || textNonNegative(e);
    }

    function validateTextPercentage(e) {
        return textPercentageBounds(e) || textInteger(e);
    }

    function validateMultiStartBeforeEnd(inputs) {
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
     * @param elements - three integers
     * @returns an error message if the sum is less than 98 or more than 102
     */
    function validateMultiSumTo100(elements) {
        if (validateTextPercentage(elements[0]) || validateTextPercentage(elements[1]) || validateTextPercentage(elements[2])) {
            return false;
        } else {
            var sum = asInteger(elements[0]) + asInteger(elements[1]) + asInteger(elements[2]);
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

    this.validations.textPositiveInteger = validateTextPositiveInteger;
    this.validations.textPercentage = validateTextPercentage;
    this.validations.multiSumTo100 = validateMultiSumTo100;
    this.validations.multiStartBeforeEnd = validateMultiStartBeforeEnd;
}

function validationPlumbing(messages) {
    var v = new Validation(messages);

    v.validateDateInput("reportDates.startDate", v.validations.dateValid);
    v.validateDateInput("reportDates.endDate", v.validations.dateValid);

    v.validateMultiple([
            "reportDates.startDate.year", "reportDates.startDate.month", "reportDates.startDate.day",
            "reportDates.endDate.year", "reportDates.endDate.month", "reportDates.endDate.day"],
        v.validations.multiStartBeforeEnd);

    v.validateTextInput("paymentHistory.averageDaysToPay", v.validations.textPositiveInteger);
    v.validateTextInput("paymentHistory.percentPaidBeyondAgreedTerms", v.validations.textPercentage);
    v.validateTextInput("paymentHistory.percentageSplit.percentWithin30Days", v.validations.textPercentage);
    v.validateTextInput("paymentHistory.percentageSplit.percentWithin60Days", v.validations.textPercentage);
    v.validateTextInput("paymentHistory.percentageSplit.percentBeyond60Days", v.validations.textPercentage);
    v.validateTextInput("paymentTerms.paymentPeriod", v.validations.textPositiveInteger);
    v.validateTextInput("paymentTerms.maximumContractPeriod", v.validations.textPositiveInteger);

    v.validateMultiple([
        "paymentHistory.percentageSplit.percentWithin30Days",
        "paymentHistory.percentageSplit.percentWithin60Days",
        "paymentHistory.percentageSplit.percentBeyond60Days"
    ], v.validations.multiSumTo100);
}
