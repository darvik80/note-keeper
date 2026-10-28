const fs = require('fs');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

class CopyFaviconPlugin {
  apply(compiler) {
    compiler.hooks.afterEmit.tap('CopyFaviconPlugin', () => {
      const from = path.resolve(__dirname, 'favicon.svg');
      const to = path.join(compiler.options.output.path, 'favicon.svg');
      fs.copyFileSync(from, to);
    });
  }
}

module.exports = (_env, argv) => {
  const isProd = argv.mode === 'production';

  return {
    mode: isProd ? 'production' : 'development',
    entry: './src/index.tsx',
    output: {
      path: path.resolve(__dirname, 'dist'),
      publicPath: '/',
      filename: isProd ? 'js/[name].[contenthash:8].js' : '[name].js',
      chunkFilename: isProd ? 'js/[id].[contenthash:8].js' : '[name].js',
      clean: true,
    },
    optimization: {
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendors',
            priority: 10,
          },
        },
      },
    },
    module: {
      rules: [
        {
          test: /\.(ts|tsx)$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: [
                ['@babel/preset-react', { development: !isProd }],
                '@babel/preset-env',
                '@babel/preset-typescript',
              ],
            },
          },
        },
        {
          test: /\.css$/,
          use: ['style-loader', 'css-loader', 'postcss-loader'],
        },
      ],
    },
    resolve: {
      extensions: ['.ts', '.tsx', '.js', '.jsx'],
    },
    devServer: {
      port: 3001,
      client: { overlay: false },
      allowedHosts: ['all', '.alibaba-inc.com'],
      static: {
        directory: path.resolve(__dirname, 'public'),
        publicPath: '/',
      },
      historyApiFallback: {
        index: '/index.html',
        rewrites: [{ from: /^\/_p\/\d+\//, to: '/index.html' }],
      },
      proxy: [
        {
          context: ['/api'],
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      ],
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: './index.html',
        inject: 'body',
      }),
      new CopyFaviconPlugin(),
    ],
  };
};
