var webpack = require('webpack');
var path = require('path');
var ExtractTextPlugin = require('extract-text-webpack-plugin');

module.exports = {
  debug: true,
  devtool: '#cheap-eval-source-map',

  resolve: {
    modulesDirectories: ['node_modules'],
    extensions: ['', '.js']
  },

  entry: [
    'webpack/hot/dev-server',
    './web/app.js'
  ],

  output: {
    path: path.join(__dirname, 'public'),
    publicPath: '/assets/',
    filename: 'bundle.js'
  },

  plugins: [
    new ExtractTextPlugin('bundle.css', {
      allChunks: true
    }),
    new webpack.optimize.OccurenceOrderPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new webpack.NoErrorsPlugin()
  ],

  devServer: {
    hot: true,
    inline: true,
    stats: { colors: true },
    historyApiFallback: true,
    proxy: {
      '*': 'http://localhost:9000'
    }
  },

  module: {
    loaders: [
      // Webpack expose both AMD and CommonJS by default
      // some packages will try to export to AMD before CommonJS
      // just to be sure, disable "define" for everyone and no more AMD
      { test: /\.js$/, loaders: ['imports?define=>false'] },
      { test: /\.js$/, exclude: /node_modules/, loaders: ['react-hot', 'babel-loader?stage=0'] },
      { test: /\.scss$/, loader: ExtractTextPlugin.extract('style', 'css?modules&importLoaders=1&localIdentName=[name]__[local]___[hash:base64:5]!sass')}
    ]
  }
};
