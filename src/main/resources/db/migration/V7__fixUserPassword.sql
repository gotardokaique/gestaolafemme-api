UPDATE usuario 
SET usu_senha = '$argon2id$v=19$m=65536,t=5,p=2$jTfp2f9aCxhMrX4FQ9kaWQ$//FWRVl8m2qao6tNb4tir+0OtGLElS5pK6NTF6pnpSWBY1KuAlD6HDDTwns+mSP5pK0'
WHERE usu_email = 'kaiquecgotardo@gmail.com';
