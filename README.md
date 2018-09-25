# kulu
Service and mobile app for handling corporate reimbursements

- The backend is written in clojure and can be found [here](https://github.com/nilenso/kulu/tree/readme/backend)
- The rails front end can be found [here](https://github.com/nilenso/kulu/tree/master/website)
- The android app can be found [here](https://github.com/nilenso/kulu/tree/master/android)

All the info on how to set up and get things started are mentioned in the readmes of the respective services.

# Update

Making this a monolith was a bad idea. We're planning to migrate these back into individual repos. Steps:

1. Remove `website` and `mobile` from this monolith repo
2. Rename this repo to become the new kulu-backend
3. Deprecate https://github.com/nilenso/kulu-backend
