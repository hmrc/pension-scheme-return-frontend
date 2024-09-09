var printLinks = document.querySelectorAll('a[href="#print"]')

if (printLinks.length > 0) {
    printLinks.forEach(element => element.addEventListener('click', function(e) {
        e.preventDefault();
        window.print();
    }))
}