# Getting Started with Create React App

This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

## Available Scripts

In the project directory, you can run:

### `npm install`

Install all dependencies before running the project.

### `npm start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

The page will reload when you make changes.\
You may also see any lint errors in the console.

### `npm run build`

Builds the app for production to the `build` folder.\
It correctly bundles React in production mode and optimizes the build for the best performance.

See the section about [deployment](https://facebook.github.io/create-react-app/docs/deployment) for more information.

## Project Structure

```
bookstore-react/
├── public/
│   └── index.html          ← HTML entry file
├── src/
│   ├── components/         ← Reusable UI components
│   │   ├── Header.js       ← Navigation header
│   │   ├── Footer.js      ← Footer component
│   │   ├── Layout.js      ← Page layout wrapper
│   │   ├── BookCard.js    ← Book card component
│   │   └── BookList.js    ← Book list grid
│   ├── pages/             ← Page-level components
│   │   ├── HomePage.js    ← Home page (book list)
│   │   ├── BookDetailPage.js ← Book detail page
│   │   ├── CartPage.js    ← Shopping cart
│   │   ├── LoginPage.js   ← Login page
│   │   └── OrderPage.js   ← Order page
│   ├── Data.js            ← Book data (like Data.json requirement)
│   ├── App.js             ← Root component & routing
│   ├── index.js           ← App entry point
│   └── index.css          ← Global styles
├── package.json
└── README.md
```

## Key Features

1. **React Router** - Client-side routing between book list and detail pages
2. **URL Parameters** - Pass book ID via URL (e.g., `/book/4` for "三体")
3. **Component-based** - Reusable components following React best practices
4. **Data from File** - All book data stored in `Data.js`, similar to `Data.json` requirement

## Learn More

You can learn more in the [Create React App documentation](https://facebook.github.io/create-react-app/docs/getting-started).

To learn React, check out the [React documentation](https://reactjs.org/).

To learn React Router, check out the [React Router documentation](https://reactrouter.com/).