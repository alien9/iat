<?php

include('PHPMailer_5.2.0/class.phpmailer.php');
$body=ob_get_clean();
$mail = new PHPMailer ();
$mail->IsSMTP ();
$mail->SMTPAuth   = true;
//$mail->SMTPSecure = 'ssl';
$mail->Host = 'smtp.galaxynotevivo.com.br';
$mail->Port  = 25;
$mail->Mailer= 'smtp';
$mail->Username   = 'contato@galaxynotevivo.com.br';
$mail->Password   = 'Cebolinha1980';
#$mail->AddReplyTo ('Reply Email', 'Recipient Name');
$mail->From = 'contato@galaxynotevivo.com.br';
$mail->FromName = 'SAMSUNG GALAXY NOTE VIVO';
$mail->Subject = "Sua Capinha para o Galaxy Note";
$mail->Body = "Você desenhou!";
$mail->WordWrap = 50;
$mail->AddAddress ('leandro@homembala.com.br', 'LEANDRO VELLOSO');
$mail->AddBCC ('barufi@gmail.com', 'Receiver Name');
$mail->addAttachment('comp.jpg');
$mail->IsHTML (true);
$mail->CharSet = 'utf-8';
$mail->Send ();
echo "mensagem enviada.\n";
