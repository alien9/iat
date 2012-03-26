<?php
error_log(print_r($_REQUEST,1));
error_log(print_r($_FILES,1));

include_once("config.php");
include_once("functions.php");

$nome = $_REQUEST["nome"];
$cpf = $_REQUEST["cpf"];
$email = $_REQUEST["email"];
$tel = $_REQUEST["telefone"];
$endereco = $_REQUEST["endereco"];
$cidade = $_REQUEST["cidade"];
$imei = $_REQUEST["imei"];
if($_REQUEST["desejareceber"]){ $desejareceber=1; }else{ $desejareceber=0; }
$background = $_REQUEST["background"];

$img = isset($_FILES["imagem"]) ? $_FILES["imagem"] : FALSE;

$sql = "INSERT INTO participantes (nome, cpf, email, tel, endereco, cidade, imei, desejareceber, background, inserido) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
$stmt = $pdo->prepare($sql);
$dados = array($nome, $cpf, $email, $tel, $endereco, $cidade, $imei, $desejareceber, $background);
error_log(print_r($dados,1));
error_log('antes');
$consultou = $stmt->execute($dados);
error_log('depois');
$id = $pdo->lastInsertId();
error_log(print_r($pdo,1));

# UPLOAD DA IMAGEM:

if ($img["tmp_name"]){
		// Tamanho máximo dos arquivos (em bytes)
		$config["tamanho"] = 1048576;
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
						echo "<br /><br /> - " . $err . "<br /><br />"; 
						exit();
				} 
		}else{
				// Faz o upload da imagem
				$nome_foto = "imagem-".$id.".png";	
				gerathumb($img["tmp_name"], "imagens/".$nome_foto, '800', '1280');
	}
}
?>