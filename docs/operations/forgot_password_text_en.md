# Configuration of the text for forgotten password

A custom message can be displayed, which appears
when "Forgot password?" is clicked. You can store information in this key how a 
user should handle it if he has forgotten his password.

This can be done by setting the etcd key `config/cas/forgot_password_text` to the 
desired value and restarting the CAS Dogu. The text can now be shown in the login screen
by clicking on "Forgot password?".
