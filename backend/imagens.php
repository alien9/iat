<?php
include_once("config.php");
$id=intval($_REQUEST['id']);
$stmt = $pdo->prepare("select background from participantes where id=?");
$stmt->execute(array($id));
$nome_foto = "imagem-".$id.".png";	
if($r=$stmt->fetch()){
system("composite ../imagens/foreground_$nome_foto ../background/low".str_pad($r['background'], 2, '0', STR_PAD_LEFT).".jpg ../output/capa.jpg");
}

$im = imagecreatefromjpeg("../output/capa.jpg");
//header('image/png');
$background = imagecolorallocate($im, 0, 0, 0);
        // removing the black from the placeholder
imagecolortransparent($im, $background);

        // turning off alpha blending (to ensure alpha channel information 
        // is preserved, rather than removed (blending with the rest of the 
        // image in the form of black))
imagealphablending($im, false);

        // turning on alpha channel information saving (to ensure the full range 
        // of transparency is preserved)
imagesavealpha($im, true);

header('Content-Type: image/jpeg');
imagejpeg($im);
imagedestroy($im);
