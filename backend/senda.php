<?php
include_once("config.php");
include_once("functions.php");
include('PHPMailer_5.2.0/class.phpmailer.php');
$sql = "select id, nome, cpf, email, tel, endereco, cidade, imei, desejareceber, background from participantes where sent=0 limit 100";

$stmt = $pdo->prepare($sql);
$consultou = $stmt->execute();

   
while($r=$stmt->fetch()){
  $nome_foto = "imagem-".$r['id'].".png";	

  system("convert ../imagens/$nome_foto -resize 800x1280\\! ../imagens/foreground_$nome_foto");
  system("composite ../imagens/foreground_$nome_foto ../background/low".str_pad($r['background'], 2, '0', STR_PAD_LEFT).".jpg ../output/comp.jpg");
$body='<html>
	<head>
	<title>Galaxy Note</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
</head>
	<body>
	Nome: '.$r['nome'].', <br/>
	Endereço: '.$r['endereco'].', <br/>
	CPF: '.$r['cpf'].', <br/>
	email: '.$r['email'].', <br/>
	Cidade: '.$r['cidade'].', <br/>
	Telefone: '.$r['tel'].', <br/>
	IMEI: '.$r['imei'].', <br/>

	
	</body>
	</html>';

    
    $mail = new PHPMailer();
    $mail->IsSMTP();
    $mail->SMTPAuth   = true;
    //$mail->SMTPSecure = 'ssl';
    $mail->Host = 'smtp.galaxynotevivo.com.br';
    $mail->Port  = 587;
    $mail->Mailer= 'smtp';
    $mail->Username   = 'contato@galaxynotevivo.com.br';
    $mail->Password   = 'Cebolinha1980';
    $mail->AddReplyTo('contato@galaxynotevivo.com.br', 'Galaxy Note VIVO');
    $mail->From = 'contato@galaxynotevivo.com.br';
    $mail->FromName = 'SAMSUNG GALAXY NOTE VIVO';
    $mail->Subject = 'Galaxy Note';
    $mail->Body = $body;
    $mail->WordWrap = 50;

    $mail->AddAddress('contato@galaxynotevivo.com.br', 'Contato');
    $mail->addAttachment("../output/comp.jpg");
    $mail->IsHTML(true);
    $mail->CharSet = 'UTF-8';
    $mail->Send();
    echo("mensagem ".$r['id']." Enviada");
    $su=$pdo->prepare("update participantes set sent=1 where id=?");
    $su->execute(array($r['id']));
}
