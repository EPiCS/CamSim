function paintBanditInfo(AllBanditInfo, AllBanditNames, scenname, NormFactor, paintNames, HeterosPainted, fileext)
    xbar_norm = AllBanditInfo(:,1)/NormFactor(1);
    ybar_norm = AllBanditInfo(:,3)/NormFactor(2);
    erbarx_norm = AllBanditInfo(:,2)/NormFactor(1);
    erbary_norm = AllBanditInfo(:,4)/NormFactor(2);
    
    hold on
   
    
    BANDITS = zeros(0,0);
    for index = 1:numel(AllBanditNames)
       n = cell2mat(AllBanditNames(index));
       BANDITS = [BANDITS; cellstr(n(1: strfind(n, '#')-1))];
    end
    
    NAMES = zeros(0,0);
    for index = 1:numel(AllBanditNames)
       n = cell2mat(AllBanditNames(index));
       NAMES = [NAMES; cellstr(n(strfind(n, '#')+1: numel(n)))];
    end
    
    %paint with different color for different solver
    cols = ['g','b', 'm', 'c']; %zeros(0,0);
    markers = ['*', 'd', 'o', 'v'];
    uniqueBandits = unique(BANDITS);
    listoflists = zeros(0,0);
    currentList = zeros(0,0);
    listofnames = zeros(0,0);
    for ub = 1:numel(uniqueBandits)
        if numel(cols) < ub
            col = rand(1,3);
            cols = [cols; col]
            marker = 'p';
        else
            col = cols(ub); 
            marker = markers(ub);
        end
        for all = 1:numel(BANDITS)
            if strcmp(BANDITS(all), uniqueBandits(ub))
                currentList = [currentList; xbar_norm(all), ybar_norm(all)];
                listofnames = [listofnames; NAMES(all)];
               %plot(xbar_norm(all), ybar_norm(all), '^', 'Color', col);
%                if(paintNames)
%                     text(xbar_norm(all), ybar_norm(all)+0.003, NAMES(all), 'Color', col);
%                     saveas(gcf, strcat('figures/bandit_', scenname(1:numel(scenname)-1), '_names.fig'), 'fig');
%                end
            end
        end
        %listoflists  = [listoflists currentList];
        plot(currentList(:,1), currentList(:,2), 'o', 'Color', col, 'MarkerSize', 14, 'LineWidth', 2, 'Marker', marker);
        if(paintNames)
            text(currentList(:,1), currentList(:,2)+0.003, listofnames, 'Color', col);
            %saveas(gcf, strcat('figures/bandit_', scenname(1:numel(scenname)-1), '_names.fig'), 'fig');
        end
        currentList = zeros(0,0);
        listofnames = zeros(0,0);
    end
    %uniqueBandits = uniqueBandits(numel(strfind(char(uniqueBandits), '/')), numel(uniqueBandits(:)));
    %uniqueBandits = ['Heterogeneous'; 'Homogeneous'; uniqueBandits];
    if HeterosPainted
        uniqueBandits = ['Heterogeneous'; 'Homogeneous'; uniqueBandits];
    else
        uniqueBandits = ['Homogeneous'; uniqueBandits];
    end
    legend(uniqueBandits, 'Location', 'Best');
    figureHandle = gcf;
    %# make all text in the figure to size 14 and bold
    set(findall(figureHandle,'type','text'),'fontSize',14)
    set(gca,'fontsize',14)
    
    saveas(gcf, strcat('figures/bandit_', scenname(1:numel(scenname)-1), '_names.', fileext), fileext);
end