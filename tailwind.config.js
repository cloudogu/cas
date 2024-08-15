/** @type {import('tailwindcss').Config} */
module.exports = {
    presets: [require('@cloudogu/ces-theme-tailwind/tailwind.presets.cjs')],
    content: [
        "app/src/main/resources/templates/**/*.html",
    ],
    theme: {
        extend: {
            screens: {
                'cas-mobile': {'max': '783px'},
                'cas-desktop': '784px',
            },
            colors: {
                "cas-logo-bg": "var(--cas-config-logo-background)",
            },
        }
    },
}

