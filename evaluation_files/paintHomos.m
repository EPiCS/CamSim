%#paintHomos find simultion runs with homogeneous nodes and paints them
%
%#RES is the matrix containing the Results to be plottet, every ROW is one scenario (result entity), every two COLUMNS is mean and std
%# ---------- ONLY first 4 COLOUMNS ARE BEING USED!!!! ------------
%#example:  meanX1 stdX1 meanY1 stdY1
%#          meanX2 stdX2 meanY2 stdY2
%
%# NAMES holds the names for the results - each row is a new scenario/result name
%# Normalized decide if results should be normalized - should depend if others are normalized as well, irrelevant if this is the only paint function used
%# scenname is the name of the scenario
%# painNames decides if names are printed as well
%# linkThem decides if the resulting nodes should be linked
%# NormFactor is the factor to normalize values - put 1 if no normalization is desired

function paintHomos(RES, NAMES, NormFactor, scenname, paintNames, linkThem, fileext)
    clf
    %find all homo-scenarios and related results
    homoRes = zeros(0,0);
    homoNames = zeros(0,0);
    for nr = 1:numel(NAMES(:,1))
       if(IsHomogeneous(NAMES(nr, :)))
           homoNames = [homoNames; NAMES(nr,:)];
           homoRes = [homoRes; RES(nr, :)];
       end
    end
    
    %plot results again
    xbar_norm = homoRes(:,1)/NormFactor(1);
    ybar_norm = homoRes(:,3)/NormFactor(2);
    erbarx_norm = homoRes(:,2)/NormFactor(1);
    erbary_norm = homoRes(:,4)/NormFactor(2);
    
    hold on
    plot(xbar_norm, ybar_norm, 'rs', 'MarkerSize', 14, 'LineWidth', 2);
    
    if(linkThem)
        line([xbar_norm(:) xbar_norm(:)], [ybar_norm(:) ybar_norm(:)], 'Color', 'red')
    end
    
    if(paintNames)
        for nr = 1:numel(homoNames(:,1))
            uniques = unique(homoNames(nr,:));
            resn = uniques(regexp(uniques, '\d'));
            resl = uniques(regexp(uniques, '\D'));

            name = '';
            if(strcmpi(resl, 'a'))
               name = strcat(name, 'Active ');
            else
                if(strcmpi(resl, 'p'))
                    name = strcat(name, 'Passive ');
                else if(strcmpi(resl, 'x'))
                    name = strcat(name, 'Anticipatory Active ');
                    else if(strcmpi(resl, 'y'))
                            name = strcat(name, 'Anticipatory Passive ');
                        end
                    end
                end
            end

            switch (resn)
               case {'0'}
                   name = strcat(name, ' Broadcast');
               case {'1'}
                   name = strcat(name, ' SMOOTH');
               case {'2'}
                   name = strcat(name, ' STEP');
                case {'3'}
                    name = strcat(name, ' FIX');
               otherwise
                   name = strcat(name, ' WHAT');
            end
            text(xbar_norm(nr), ybar_norm(nr)+0.003, name, 'Color', 'red');
        end
    end
    
    saveas(gcf, strcat('figures/perf', scenname(1:numel(scenname)-1), 'homos.', fileext), fileext);
    hold off
end