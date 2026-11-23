// Karma configuration personalizada (ver doc 04 \u00a714 y D19).
//
// Genera reporte de cobertura cada vez que se corre `ng test`. El
// mínimo global del 30% se verifica con `scripts/check-coverage-min.js`,
// invocado por `npm test` tras ng test (ver package.json). No hay techo.
//
// Angular 19 usa @angular-devkit/build-angular:karma como builder. Este
// archivo se pasa v\u00eda angular.json.architect.test.options.karmaConfig.

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma'),
    ],
    client: {
      jasmine: {},
      clearContext: false,
    },
    jasmineHtmlReporter: { suppressAll: true },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'json-summary', file: 'coverage-summary.json' },
      ],
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['ChromeHeadless'],
    singleRun: false,
    restartOnFileChange: true,
  });
};
