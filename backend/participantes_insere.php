<?php
include_once("config.php");
include_once("functions.php");
error_log(print_r($_REQUEST,1));
$nome = $_REQUEST["nome"];
$cpf = $_REQUEST["cpf"];
$email = $_REQUEST["email"];
$tel = $_REQUEST["telefone"];
$endereco = $_REQUEST["endereco"];
$cidade = $_REQUEST["cidade"];
$imei = $_REQUEST["imei"];
if($_REQUEST["capa"]){ $desejareceber=1; }else{ $desejareceber=0; }
$background = $_REQUEST["background"];

$img = isset($_FILES["imagem"]) ? $_FILES["imagem"] : FALSE;

$sql = "INSERT INTO participantes(nome, cpf, email, tel, endereco, cidade, imei, desejareceber, background, inserido) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
$stmt = $pdo->prepare($sql);
$dados = array($nome, $cpf, $email, $tel, $endereco, $cidade, $imei, $desejareceber, $background);
error_log(print_r($dados,1));
$consultou = $stmt->execute($dados);
$id = $pdo->lastInsertId();

$config=array();
# UPLOAD DA IMAGEM:

if($img["tmp_name"]){
		// Tamanho máximo dos arquivos(em bytes)
		$config["tamanho"] = 10048576;
		// Verifica se o mime-type do arquivo é de imagem
		if(!preg_match("/\.(gif|bmp|png|jpg|jpeg){1}$/i", $img["name"], $ext)){
				$erro[] = "Arquivo em formato inválido!";
		}
		// Verifica tamanho do arquivo
		if($i["size"] > $config["tamanho"]){
				$erro[] = "Arquivo em tamanho muito grande! A imagem deve ser de no máximo " . $config["tamanho"] . " bytes.";
		}
		//Verifica se houve erro
		if(sizeof($erro)) { 
				foreach($erro as $err) { 
				    error_log($err);
						echo "<br /><br /> - " . $err . "<br /><br />"; 
				} 
				exit();
		}else{
				// Faz o upload da imagem
				$nome_foto = "imagem-".$id.".png";	
				gerathumb($img["tmp_name"], "../imagens/".$nome_foto, '800', '1280');
/*
				$ficura='<image
     xlink:href="imagens/'.$nome_foto.'"
     x="-96.716019"
     y="-96.112076"
     width="407.92758"
     height="607.04358"
     id="image9559" />';
        $rh = fopen('vetores/arte'.intval($background).'.svg', "r");
        $wh = fopen('../output/comp.svg', 'w');
        while($l=fgets($rh)) fwrite($wh, str_replace('</svg>', $ficura.'</svg>', $l));
        fclose($rh);
        fclose($wh);
*/
   if(!$desejareceber) exit;
$body='<html>
	<head>
	<title>Galaxy Note</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
</head>
	<body>'.$nome.', <br/>
O seu cadastro foi realizado com sucesso. A Samsung e a Vivo agradecem a sua participação!<br/>
Em até 30 dias, você receberá a sua capa no endereço cadastrado.<br/>
Em caso de dúvida, envie um email para contato@galaxynotevivo.com.br.<br/>
<br/>
Obrigado.</body>
	</html>';

    system("convert ../imagens/$nome_foto -resize 800x1280\\! ../imagens/foreground_$nome_foto");
    system("composite ../imagens/foreground_$nome_foto ../background/low".str_pad($background, 2, '0', STR_PAD_LEFT).".jpg ../output/capa.jpg");

    // Let's write the image. 
    //if  (FALSE == $face->writeImage()) 
        #$bg=file_get_contents('vetores/arte'.intval($background).'.svg');
        #file_put_contents('../output/comp.svg', str_replace('</svg>', $ficura.'</svg>', $bg));
        include('PHPMailer_5.2.0/class.phpmailer.php');
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
        $mail->Subject = '=?UTF-8?B?{'.base64_encode("Galaxy Note").'}?=';
        $mail->Body = $body;
        $mail->WordWrap = 50;
        $mail->AddAddress($email, $nome);
        $mail->AddBCC('barufi@gmail.com', 'Receiver Name');
        $mail->addAttachment('../output/capa.jpg');
        $mail->IsHTML(true);
        $mail->CharSet = 'UTF-8';
        $mail->Send();
        error_log("mensagem Enviada");
	}
}

