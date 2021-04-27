// used by the cookie banner component

(function (root) {
  'use strict';
  window.GOVUK = window.GOVUK || {};

  var DEFAULT_COOKIE_CONSENT = {
    'analytics': false
  };

  var COOKIE_CATEGORIES = {
    '_ga': 'analytics',
    '_gid': 'analytics'
  };

  /*
    Cookie methods
    ==============
    Usage:
      Setting a cookie:
      GOVUK.cookie('hobnob', 'tasty', { days: 30 });
      Reading a cookie:
      GOVUK.cookie('hobnob');
      Deleting a cookie:
      GOVUK.cookie('hobnob', null);
  */
  window.GOVUK.cookie = function (name, value, options) {
    if (typeof value !== 'undefined') {
      if (value === false || value === null) {
        return window.GOVUK.setCookie(name, '', { days: -1 });
      } else {
        // Default expiry date of 30 days
        if (typeof options === 'undefined') {
          options = { days: 30 };
        }
        return window.GOVUK.setCookie(name, value, options);
      }
    } else {
      return window.GOVUK.getCookie(name);
    }
  };

  window.GOVUK.getConsentCookie = function () {
    var consentCookie = window.GOVUK.cookie('cookies_policy');
    var consentCookieObj;

    if (consentCookie) {
      try {
        consentCookieObj = JSON.parse(consentCookie);
      } catch (err) {
        return null;
      }

      if (typeof consentCookieObj !== 'object' && consentCookieObj !== null) {
        consentCookieObj = JSON.parse(consentCookieObj);
      }
    } else {
      return null;
    }

    return consentCookieObj;
  };

  window.GOVUK.setConsentCookie = function (options) {
    var cookieConsent = window.GOVUK.getConsentCookie();

    if (!cookieConsent) {
      cookieConsent = JSON.parse(JSON.stringify(DEFAULT_COOKIE_CONSENT));
    }

    for (var cookieType in options) {
      cookieConsent[cookieType] = options[cookieType];

      // Delete cookies of that type if consent being set to false
      if (!options[cookieType]) {
        for (var cookie in COOKIE_CATEGORIES) {
          if (COOKIE_CATEGORIES[cookie] === cookieType) {
            window.GOVUK.cookie(cookie, null);

            if (window.GOVUK.cookie(cookie)) {
              document.cookie = cookie + '=;expires=' + new Date() + ';domain=' + window.location.hostname.replace(/^www\./, '.') + ';path=/';
            }
          }
        }
      }
    }
    window.GOVUK.setCookie('cookies_policy', JSON.stringify(cookieConsent), { days: 365 });
    window.GOVUK.showConfirmationMessage(options.analytics);
  };

  window.GOVUK.checkConsentCookieCategory = function (cookieName, cookieCategory) {
    var currentConsentCookie = window.GOVUK.getConsentCookie();

    // If the consent cookie doesn't exist, but the cookie is in our known list, return true
    if (!currentConsentCookie && COOKIE_CATEGORIES[cookieName]) {
      return true;
    }

    currentConsentCookie = window.GOVUK.getConsentCookie();

    // Sometimes currentConsentCookie is malformed in some of the tests, so we need to handle these
    try {
      return currentConsentCookie[cookieCategory];
    } catch (e) {
      console.error(e);
      return false;
    }
  };

  window.GOVUK.checkConsentCookie = function (cookieName, cookieValue) {
    // If we're setting the consent cookie OR deleting a cookie, allow by default
    if (cookieName === 'cookies_policy' || (cookieValue === null || cookieValue === false)) {
      return true;
    }

    if (COOKIE_CATEGORIES[cookieName]) {
      var cookieCategory = COOKIE_CATEGORIES[cookieName];

      return window.GOVUK.checkConsentCookieCategory(cookieName, cookieCategory);
    } else {
      // Deny the cookie if it is not known to us
      return false;
    }
  };

  window.GOVUK.setCookie = function (name, value, options) {
    if (window.GOVUK.checkConsentCookie(name, value)) {
      if (typeof options === 'undefined') {
        options = {};
      }
      var cookieString = name + '=' + value + '; path=/';
      if (options.days) {
        var date = new Date();
        date.setTime(date.getTime() + (options.days * 24 * 60 * 60 * 1000));
        cookieString = cookieString + '; expires=' + date.toGMTString();
      }
      if (document.location.protocol === 'https:') {
        cookieString = cookieString + '; Secure';
      }
      document.cookie = cookieString;
    }
  };

  window.GOVUK.getCookie = function (name) {
    var nameEQ = name + '=';
    var cookies = document.cookie.split(';');
    for (var i = 0, len = cookies.length; i < len; i++) {
      var cookie = cookies[i];
      while (cookie.charAt(0) === ' ') {
        cookie = cookie.substring(1, cookie.length);
      }
      if (cookie.indexOf(nameEQ) === 0) {
        return decodeURIComponent(cookie.substring(nameEQ.length));
      }
    }
    return null;
  };

  window.GOVUK.showConfirmationMessage = function (analyticsConsent) {
    var messagePrefix = analyticsConsent ? 'You’ve accepted analytics cookies.' : 'You told us not to use analytics cookies.';
    var cookieBannerMainContent = document.querySelector('.notify-cookie-banner__wrapper');
    var cookieBannerConfirmation = document.querySelector('.notify-cookie-banner__confirmation');
    var cookieBannerConfirmationMessage = document.querySelector('.notify-cookie-banner__confirmation-message');

    cookieBannerConfirmationMessage.insertAdjacentText('afterbegin', messagePrefix);
    cookieBannerMainContent.style.display = 'none';
    cookieBannerConfirmation.style.display = 'block';
  }

  window.GOVUK.hideCookieMessage = function (event) {
    var cookieBannerConfirmation = document.querySelector('.notify-cookie-banner__confirmation');
        cookieBannerConfirmation.style.display = 'none';

    if (event.target) {
      event.preventDefault();
    }
  };

  window.GOVUK.showCookieMessage = function () {
      // Show the cookie banner if not in the cookie settings page
      var hasCookiesPolicy = window.GOVUK.getConsentCookie();
      var cookieBannerMainContent = document.querySelector('.notify-cookie-banner__wrapper');
      if (!hasCookiesPolicy) {
        cookieBannerMainContent.style.display = 'block';
      } else {
          cookieBannerMainContent.style.display = 'none';
      }
  };

  window.GOVUK.isInCookiesPage = function () {
    return window.location.pathname === '/cookies';
  };

}(window));


(function() {
  "use strict"
    var cookieBanner = document.querySelector('.notify-cookie-banner');
    if (window.GOVUK.isInCookiesPage() || window.GOVUK.getConsentCookie() !== null) {
        cookieBanner.style.display = 'none';
    } else if (window.GOVUK.getConsentCookie() === null) {
        cookieBanner.style.display = 'block';
    }



    var hideLink = document.querySelector('button[data-hide-cookie-banner]');
    if (hideLink) {
      hideLink.addEventListener('click', window.GOVUK.hideCookieMessage);
    }

    var acceptCookiesLink = document.querySelector('button[data-accept-cookies=true]');
    if (acceptCookiesLink) {
      acceptCookiesLink.addEventListener('click', () => window.GOVUK.setConsentCookie({'analytics': true}));

    }

    var rejectCookiesLink = document.querySelector('button[data-accept-cookies=false]');
    if (rejectCookiesLink) {
      rejectCookiesLink.addEventListener('click', () => window.GOVUK.setConsentCookie({'analytics': false}));
    }

    var saveCookieBtn = document.querySelector('.govuk-save-cookie-settings');
    var cookieSettingsConfirmation = document.querySelector('.cookie-settings__confirmation');

    var cookieAnalyticsAccept = document.querySelector('#cookies-analytics-yes');
    var cookieAnalyticsDecline = document.querySelector('#cookies-analytics-no');

    if (saveCookieBtn) {
        saveCookieBtn.addEventListener('click', function (event) {
        event.preventDefault();
        cookieSettingsConfirmation.style.display = 'block';

        if (cookieAnalyticsAccept.checked) {
            window.GOVUK.setConsentCookie({'analytics': true});

            (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
                m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
                })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

                ga('create', 'UA-93179138-1', 'auto');
                ga('send', 'pageview');

        } else if (cookieAnalyticsDecline.checked) {
            window.GOVUK.cookie('_ga', null);
            window.GOVUK.cookie('_gid', null);
            window.GOVUK.cookie('_gat', null);
            window.GOVUK.setConsentCookie({'analytics': false});
        }
      });
    }

    if (window.GOVUK.isInCookiesPage()) {
        var previousPageLink = document.querySelector(".cookie-settings__prev-page");

        if (previousPageLink) {
            previousPageLink.href = document.referrer;
        }
    }


  // header navigation toggle
  if (document.querySelectorAll && document.addEventListener){
    var els = document.querySelectorAll('.js-header-toggle'),
        i, _i;
    for(i=0,_i=els.length; i<_i; i++){
      els[i].addEventListener('click', function(e){
        e.preventDefault();
        var target = document.getElementById(this.getAttribute('href').substr(1)),
            targetClass = target.getAttribute('class') || '',
            sourceClass = this.getAttribute('class') || '';

        if(targetClass.indexOf('js-visible') !== -1){
          target.setAttribute('class', targetClass.replace(/(^|\s)js-visible(\s|$)/, ''));
        } else {
          target.setAttribute('class', targetClass + " js-visible");
        }
        if(sourceClass.indexOf('js-hidden') !== -1){
          this.setAttribute('class', sourceClass.replace(/(^|\s)js-hidden(\s|$)/, ''));
        } else {
          this.setAttribute('class', sourceClass + " js-hidden");
        }
      });
    }
  }
}).call(this);



