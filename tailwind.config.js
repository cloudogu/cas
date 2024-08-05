/** @type {import('tailwindcss').Config} */
module.exports = {
  presets: [ require('@cloudogu/ces-theme-tailwind/tailwind.presets.cjs') ],
  content: [
      "app/src/main/resources/templates/**/*.html"
  ],
}

