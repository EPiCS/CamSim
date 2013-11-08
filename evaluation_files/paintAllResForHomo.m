function paintAllResForHomo(RES, homoNames, AllStepRes, Normalized, paintNames, runs)
    if(Normalized)
        xbar_max = RES(:,1) + RES(:,2);
        ybar_max = RES(:,3) + RES(:,4);
                                                
        normFactorX = norm(RES(:,1), inf);
        normFactorY = norm(RES(:,3), inf);

        xbar_norm = AllStepRes(:,1)/normFactorX;
        ybar_norm = AllStepRes(:,3)/normFactorY;
        
        erbarx_norm = AllStepRes(:,2)/normFactorX;
        erbary_norm = AllStepRes(:,4)/normFactorY;
    else
        xbar_norm = AllStepRes(:,1);
        ybar_norm = AllStepRes(:,3);
        erbarx_norm = AllStepRes(:,2);
        erbary_norm = AllStepRes(:,4);
    end
    hold on
    plot(xbar_norm, ybar_norm, '^g');

    if(paintNames)
        for total = 1:numel(xbar_norm)-1
            %floor(total./runs)
            %strcat(homoNames(floor(total./runs)+1,:), '_', int2str(mod(total,runs)+1))
            text(xbar_norm(total), ybar_norm(total)+0.003, strcat(homoNames(floor(total./runs)+1,:), '_', int2str(mod(total,runs)+1)));
        end
    end
    
end
