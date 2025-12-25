import { ApolloClient, InMemoryCache, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import UploadHttpLink from 'apollo-upload-client/UploadHttpLink.mjs';

const uploadLink = new UploadHttpLink({
  uri: '/graphql',
});

// Auth link that adds JWT token to every request
const authLink = setContext((_, { headers }) => {
  // Get the authentication token from local storage
  const token = localStorage.getItem('token');

  // Return the headers to the context so httpLink can read them
  return {
    headers: {
      ...headers,
      authorization: token ? `Bearer ${token}` : '',
    }
  };
});

export const apolloClient = new ApolloClient({
  link: from([authLink, uploadLink]),
  cache: new InMemoryCache(),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network',
    },
  },
});
