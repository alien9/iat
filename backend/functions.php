<?

//-------------------------------------------------------------------------------
//FUNÇÃO PARA GERAR THUMBNAILS OU REDIMENSIONAR IMAGENS
function gerathumb($photo, $output, $new_width, $max_height){
    
    $source = imagecreatefromstring(file_get_contents($photo));
    list($width, $height) = getimagesize($photo);
    if ($width>$new_width){
			
        $new_height = ($new_width/$width) * $height;
				if($new_height>$max_height){ $new_height=$max_height; } //limitando a altura
        $thumb = imagecreatetruecolor($new_width, $new_height);
        imagecopyresampled($thumb, $source, 0, 0, 0, 0, $new_width, $new_height, $width, $height);
        imagejpeg($thumb, $output, 100);
				
    }else{
			
        copy($photo, $output);
				
    }
}

?>
