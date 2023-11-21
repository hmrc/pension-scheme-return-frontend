// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function (event) {

    // handle back click
    const backLink = document.querySelector('.govuk-back-link');
    if (backLink !== null) {
        backLink.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            window.history.back();
        });
    }

    // handle country picker
    const locationElement = document.querySelector(".location-autocomplete");
    if (locationElement !== null) {
        openregisterLocationPicker({
            selectElement: locationElement,
            url: '/pension-scheme-return/assets/location-autocomplete-graph.json'
        })
    }

});
