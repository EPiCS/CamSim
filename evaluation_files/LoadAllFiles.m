function [Mfull, MCommStep, MUtilStep, MVisStep] = LoadAllFiles(directoryname)
    
    alloutputs = dir(directoryname);
    Mfull = zeros(0,0);
    MCommStep = zeros(0,0);
    MUtilStep = zeros(0,0);
    MVisStep = zeros(0,0);
    
    Mfull = zeros(0,0);
    %open each file
    for outputindex = 3:numel(alloutputs)
        if alloutputs(outputindex).isdir == 0
           %load content of each file in Mintermed
           %matrix
           if(~strcmp('summary.csv',alloutputs(outputindex).name))
               currentFile = strcat(directoryname, '/', alloutputs(outputindex).name);
               Mintermed = dlmread(currentFile, ';', 1, 1);
               %concat intermediate matrix with full matrix
               Mfull = [Mfull; Mintermed];
               MCommStep = [MCommStep Mintermed(:, 2)];
               MUtilStep = [MUtilStep Mintermed(:,1)];
               MVisStep = [MVisStep Mintermed(:,3)];
           end
        end            
    end
end
