$(document).ready(->
  $('#team-signup-form').formValidation
    message: 'This value is not valid'

    icon:
      valid: 'glyphicon glyphicon-ok'
      invalid: 'glyphicon glyphicon-remove'
      validating: 'glyphicon glyphicon-refresh'

    fields:
      organization_name:
        message: 'The Team Name is not valid'
        validators:
          notEmpty: message: 'The Team Name is required and can\'t be empty'
          stringLength:
            min: 3
            max: 15
            message: 'The Team Name must be more than 3 and less than 15 characters long'
          regexp:
            regexp: /^[a-z0-9-]+$/
            message: 'The Team Name can only consist of lowercase alphabet, numbers and dashes'

      user_name:
        message: 'The username is not valid'
        validators:
          notEmpty: message: 'The username is required and can\'t be empty'
          stringLength:
            min: 4
            max: 30
            message: 'The username must be more than 4 and less than 30 characters long'
          regexp:
            regexp: /^[a-zA-Z0-9_\.]+$/
            message: 'The username can only consist of lowercase alphabet, numbers, and dots.'

      user_email:
        validators:
          notEmpty: message: 'The email address is required and can\'t be empty'
          emailAddress: message: 'The input is not a valid email address'

      password:
        validators:
          notEmpty:
            message: 'The password is required and can\'t be empty'

      confirm:
        validators:
          notEmpty:
            message: 'The confirm password is required and can\'t be empty'
          identical:
            field: 'password',
            message: 'Passwords do not match'

  $('#team-signin-form').formValidation
    message: 'This value is not valid'

    icon:
      valid: 'glyphicon glyphicon-ok'
      invalid: 'glyphicon glyphicon-remove'
      validating: 'glyphicon glyphicon-refresh'

    fields:
      team_name:
        message: 'The Team Name is not valid'
        validators:
          notEmpty: message: 'The Team Name is required and can\'t be empty'
          stringLength:
            min: 3
            max: 15
            message: 'The Team Name must be more than 3 and less than 15 characters long'
          regexp:
            regexp: /^[a-z0-9-]+$/
            message: 'The Team Name can only consist of lowercase alphabet, numbers and dashes'

  $('#user-signup-form').formValidation
    message: 'This value is not valid'

    icon:
      valid: 'glyphicon glyphicon-ok'
      invalid: 'glyphicon glyphicon-remove'
      validating: 'glyphicon glyphicon-refresh'

    fields:
      user_name:
        message: 'The username is not valid'
        validators:
          notEmpty: message: 'The username is required and can\'t be empty'
          stringLength:
            min: 4
            max: 30
            message: 'The username must be more than 4 and less than 30 characters long'
          regexp:
            regexp: /^[a-zA-Z0-9_\.]+$/
            message: 'The username can only consist of lowercase alphabet, numbers, and dots.'

      user_email:
        validators:
          notEmpty: message: 'The email address is required and can\'t be empty'
          emailAddress: message: 'The input is not a valid email address'

      password:
        validators:
          notEmpty:
            message: 'The password is required and can\'t be empty'

      confirm:
        validators:
          notEmpty:
            message: 'The confirm password is required and can\'t be empty'
          identical:
            field: 'password',
            message: 'Passwords do not match'

  $('#forgot-password-form').formValidation
    message: 'This value is not valid'

    icon:
      valid: 'glyphicon glyphicon-ok'
      invalid: 'glyphicon glyphicon-remove'
      validating: 'glyphicon glyphicon-refresh'

    fields:
      team_name:
        message: 'The Team Name is not valid'
        validators:
          notEmpty: message: 'The Team Name is required and can\'t be empty'
          stringLength:
            min: 3
            max: 15
            message: 'The Team Name must be more than 3 and less than 15 characters long'
          regexp:
            regexp: /^[a-z0-9-]+$/
            message: 'The Team Name can only consist of lowercase alphabet, numbers and dashes'
      user_email:
        validators:
          notEmpty: message: 'The email address is required and can\'t be empty'
          emailAddress: message: 'The input is not a valid email address'

  $('#update-password-form').formValidation
    message: 'This value is not valid'

    icon:
      valid: 'glyphicon glyphicon-ok'
      invalid: 'glyphicon glyphicon-remove'
      validating: 'glyphicon glyphicon-refresh'

    fields:
      password:
        validators:
          notEmpty:
            message: 'The password is required and can\'t be empty'

      confirm:
        validators:
          notEmpty:
            message: 'The confirm password is required and can\'t be empty'
          identical:
            field: 'password',
            message: 'Passwords do not match'

  $('#login-form').formValidation
    message: 'This value is not valid'

    icon:
      valid: 'glyphicon glyphicon-ok'
      invalid: 'glyphicon glyphicon-remove'
      validating: 'glyphicon glyphicon-refresh'

    fields:
      user_email:
        validators:
          notEmpty: message: 'The email address is required and can\'t be empty'
          emailAddress: message: 'The input is not a valid email address'

      password:
        validators:
          notEmpty:
            message: 'The password is required and can\'t be empty'
);
