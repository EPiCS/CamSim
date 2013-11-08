    
function plotCumsums(FullScenRes, AllScenNames, scenname, fileext)
    clf
    renamed = cell(0);
    
    %rename internal naming to readable nameing for legend
    for nr = 1:numel(AllScenNames(:,1))
        uniques = unique(AllScenNames(nr,:));
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
        renamed = [renamed; name];
    end

    % cumulated sum for communication
    plot(cumsum(FullScenRes(:,1:4:end)));
    legend(renamed, 'Location', 'Best');
    title(strcat('communication in ', scenname));
    saveas(gcf, strcat('figures/cs_comm_', scenname, '_names.', fileext), fileext);

    % cumulated sum for utility
    plot(cumsum(FullScenRes(:,3:4:end)));
    legend(renamed, 'Location', 'Best');
    title(strcat('utility in ', scenname));
    saveas(gcf, strcat('figures/cs_util_', scenname, '_names.', fileext), fileext);
end

    