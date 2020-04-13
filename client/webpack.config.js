const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const webpack = require('webpack');

const contextPath = '/chat';

module.exports = (env, argv) => {
   const mode = argv.mode || 'development';
   return {
       entry: {
          app: [ './src/scss/main.scss', path.resolve(__dirname, './src/index.js') ]
       },
       output: {
          path: path.resolve(__dirname, './build/'),
          filename: 'resources/[name].[hash].js',
          publicPath: contextPath
       },
       node: {
          fs: 'empty'
       },
       module: {
          rules: [{
             test: /\.elm$/,
             exclude: [/elm-stuff/, /node_modules/],
             use: [
                ... (mode === 'development') ? [{
                   loader: 'elm-hot-webpack-loader'
                }] : [],
                {
                   loader: 'elm-webpack-loader',
                   options: {
                      cwd: __dirname,
                      pathToElm: 'node_modules/.bin/elm',
                      debug: mode === 'development',
                      optimize: mode === 'production'
                }
            }]
          },
          {
             test: /\.s[c|a]ss$/,
             use: [MiniCssExtractPlugin.loader, 'css-loader', 'sass-loader']
          },
          {
             test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
             loader: 'url-loader',
             options: {
                 limit: 10000,
                 mimetype: 'application/font-woff',
                 name: '[name].[contenthash].[ext]',
                 outputPath: 'resources',
                 publicPath: contextPath + '/resources'
             }
          },
          {
             test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
             loader: 'file-loader',
             options: {
                 name: '[name].[contenthash].[ext]',
                 outputPath: 'resources',
                 publicPath: contextPath + '/resources'
             }
          }]
       },
       plugins: [
          new HtmlWebpackPlugin({
             template: 'src/index.ejs',
             filename: 'index.html',
             title: 'Chat',
             chunks: [ 'app' ]
          }),
          new MiniCssExtractPlugin({
             filename: 'resources/styles.[contenthash].css'
          }),
          // For server-side SockJS service
          new CopyWebpackPlugin([
            { from: './node_modules/sockjs-client/dist/sockjs.min.js', to: 'resources' }
          ]),
          ... (mode === 'development') ? [new webpack.HotModuleReplacementPlugin()] : []
       ],
       watchOptions: {
           poll: true
       },
       optimization: {
           // minimizer only applicable in production mode.
           minimizer: [
               new TerserPlugin({
                   parallel: true
               }),
               new OptimizeCSSAssetsPlugin()
           ]
       },
       performance: {
           hints: false
       },
       mode: mode,
       devServer: {
           inline: true,
           hot: true,
           stats: 'errors-only',
           historyApiFallback: {
              index: contextPath + '/index.html'
           },
           proxy: {
               '/api': 'http://localhost:8080',
               '/ws': {
                   target: 'http://localhost:8080',
                   ws: true
               }
           },
           host: '0.0.0.0',
           port: 3000
       }
   };
};
